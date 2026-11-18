package com.example.natureaura.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.natureaura.connectivity.CameraPpgAnalyzer
import com.example.natureaura.connectivity.FrontCameraRppgAnalyzer
import com.example.ui.theme.SanctuaryVoid
import java.util.concurrent.Executors

/**
 * CameraX surface supporting:
 * - Front-facing Portrait Preview + ImageCapture + Contactless Front rPPG ImageAnalysis.
 * - Optional Rear-camera + Torch ImageAnalysis (CameraPpgAnalyzer) for finger PPG pulse mode.
 * - Graceful Sanctuary backdrop fallback if camera permission/hardware is unavailable.
 */
@Composable
fun FrontCameraPreview(
    hasCameraPermission: Boolean,
    isCameraPpgActive: Boolean,
    ppgAnalyzer: CameraPpgAnalyzer,
    frontRppgAnalyzer: FrontCameraRppgAnalyzer,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraUnavailable by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission && !cameraUnavailable) {
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            DisposableEffect(lifecycleOwner, previewView, isCameraPpgActive) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                val analysisExecutor = Executors.newSingleThreadExecutor()
                var boundProvider: ProcessCameraProvider? = null

                providerFuture.addListener(
                    {
                        runCatching {
                            val provider = providerFuture.get()
                            boundProvider = provider
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            provider.unbindAll()

                            if (isCameraPpgActive && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { it.setAnalyzer(analysisExecutor, ppgAnalyzer) }

                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis,
                                )
                                camera.cameraControl.enableTorch(true)
                                onImageCaptureReady(null)
                            } else {
                                val imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                val frontAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { it.setAnalyzer(analysisExecutor, frontRppgAnalyzer) }

                                // Bind 3 streams (Preview + Capture + Front rPPG); fall back to 2 streams on LEGACY devices
                                val boundThree = runCatching {
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        imageCapture,
                                        frontAnalysis,
                                    )
                                }.isSuccess

                                if (!boundThree) {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                                }
                                onImageCaptureReady(imageCapture)
                            }
                        }.onFailure {
                            cameraUnavailable = true
                            onImageCaptureReady(null)
                        }
                    },
                    executor,
                )

                onDispose {
                    onImageCaptureReady(null)
                    boundProvider?.unbindAll()
                    analysisExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to SanctuaryVoid.copy(alpha = 0.62f),
                                0.25f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1.0f to SanctuaryVoid.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_aura_sanctuary_bg),
                contentDescription = "Sanctuary ambient backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SanctuaryVoid.copy(alpha = 0.35f)),
            )
        }
    }
}
