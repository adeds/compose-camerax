package me.pengj.arcompose

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberImagePainter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SimpleCameraPreview(
    cameraClick: (ImageCapture, Executor) -> Unit,
    image: Uri? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { setupImageCapture() }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutine = rememberCoroutineScope()

    coroutine.launch {
        if (image == null) {
            sheetState.hide()
        } else {
            sheetState.show()
        }
    }

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        content = mainContent(
            lifecycleOwner,
            cameraProviderFuture,
            imageCapture,
            executor,
            cameraClick
        ),
        sheetContent = sheetContent(image),
        sheetState = sheetState,
        sheetElevation = 50.dp,
        sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    )
}

fun sheetContent(image: Uri?): @Composable() (ColumnScope.() -> Unit) = {
    Column {
        Image(painter = rememberImagePainter(image), contentDescription = null)
    }
}

fun mainContent(
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    imageCapture: ImageCapture,
    executor: Executor, cameraClick: (ImageCapture, Executor) -> Unit
): @Composable () -> Unit = {
    Box(contentAlignment = Alignment.BottomCenter) {
        AndroidView(
            factory = { ctx ->
                val preview = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(
                        lifecycleOwner,
                        preview,
                        cameraProvider,
                        imageCapture
                    )
                }, executor)
                preview
            },
            modifier = Modifier.fillMaxSize(),
        )
        Button(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onClick = {
                cameraClick(imageCapture, executor)
            },
            content = {
                Text(text = "ini button")
            }
        )
    }
}

private fun bindPreview(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    imageCapture: ImageCapture,
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        setupImageAnalysis(),
        imageCapture,
        preview
    )
}

private fun setupImageAnalysis(): ImageAnalysis {
    return ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
}

private fun setupImageCapture(): ImageCapture {
    return ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
}
