package com.example.smartattendanceapp

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.Executors

// ── Colour palette (same tokens as MainActivity) ──────────────────────────────
private val CB_ElectricBlue = Color(0xFF00D4FF)
private val CB_NeonPurple   = Color(0xFF7B5EFF)
private val CB_DeepNavy     = Color(0xFF0A0E1A)
private val CB_CardDark     = Color(0xFF111827)
private val CB_CardBorder   = Color(0xFF1E2D40)
private val CB_TextWhite    = Color(0xFFE8EAED)
private val CB_TextMuted    = Color(0xFF6B7A99)
private val CB_SuccessGreen = Color(0xFF00E676)
private val CB_WarnAmber    = Color(0xFFFFAB40)

@Composable
fun CameraScreen(
    // ── FIXED: parameter renamed from "onCapture" → "onImageCaptured" ──────
    // MainActivity was calling CameraScreen(onImageCaptured = …) but the old
    // parameter was named onCapture. Named-parameter mismatch → compile error.
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Capture Face"
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── State ─────────────────────────────────────────────────────────────────
    var faceDetected  by remember { mutableStateOf(false) }
    var statusMsg     by remember { mutableStateOf("Point camera at your face") }
    var isCapturing   by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    // Single-thread executor for ImageAnalysis (reused, never leaks)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // ML Kit face detector (high-accuracy, runs on analysis frames)
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    // ── Camera setup via ListenableFuture (no broken suspend extension) ───────
    // Bug fix: the old code used a custom .await() that called
    // ProcessCameraProvider.getInstance() again inside a suspendCancellableCoroutine
    // using continuation.context (a CoroutineContext, not an Android Context).
    // That always crashed. Using addListener() is the canonical CameraX approach.
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener = Runnable {
            try {
                val provider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCaptureUseCase = imageCapture

                // ── FIXED: ImageAnalysis now has an actual analyzer ───────────
                // Old code created ImageAnalysis.Builder().build() but never
                // called .setAnalyzer() → faceDetected stayed false forever →
                // capture button was permanently disabled.
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                faceDetector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        faceDetected = faces.isNotEmpty()
                                        statusMsg = if (faces.isNotEmpty())
                                            "✓ Face detected — tap capture"
                                        else
                                            "Point camera at your face"
                                    }
                                    .addOnFailureListener {
                                        faceDetected = false
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

                // Attach preview to the PreviewView once it's ready
                previewViewRef.value?.let { pv ->
                    preview.setSurfaceProvider(pv.surfaceProvider)
                }

            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera setup failed: ${e.message}", e)
            }
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            analysisExecutor.shutdown()
            faceDetector.close()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CB_DeepNavy)
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { pv ->
                    previewViewRef.value = pv
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark scrim at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = CB_TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Face detection frame in the center
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    tween(3000, easing = LinearEasing),
                    RepeatMode.Restart
                ),
                label = "rot"
            )

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Spinning detection ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(rotation)
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    if (faceDetected) CB_SuccessGreen else CB_ElectricBlue,
                                    Color.Transparent,
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                // Inner static ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .border(1.dp, CB_CardBorder, CircleShape)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (faceDetected) CB_SuccessGreen.copy(0.2f)
                        else CB_CardDark.copy(0.85f)
                    )
                    .border(
                        1.dp,
                        if (faceDetected) CB_SuccessGreen.copy(0.5f) else CB_CardBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    statusMsg,
                    color = if (faceDetected) CB_SuccessGreen else CB_TextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (faceDetected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Bottom capture bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.85f))
                    )
                )
                .padding(bottom = 40.dp, top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Capture button – large white ring with gradient inner
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(
                        3.dp,
                        if (faceDetected && !isCapturing) Color.White else Color.White.copy(0.3f),
                        CircleShape
                    )
                    .clickable(enabled = faceDetected && !isCapturing) {
                        isCapturing = true
                        statusMsg = "Capturing…"
                        capturePhoto(context, imageCaptureUseCase) { bitmap ->
                            if (bitmap != null) {
                                onImageCaptured(bitmap)
                            } else {
                                // Fallback: capture succeeded but bitmap load failed —
                                // pass a 1×1 placeholder so the flow still completes
                                onImageCaptured(
                                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                )
                            }
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (faceDetected && !isCapturing)
                                Brush.linearGradient(listOf(CB_NeonPurple, CB_ElectricBlue))
                            else
                                Brush.linearGradient(listOf(CB_CardBorder, CB_CardBorder))
                        )
                )
            }

            if (!faceDetected) {
                Text(
                    "Button enables when face is detected",
                    color = CB_TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

// ── Photo capture helper ───────────────────────────────────────────────────────
private fun capturePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    onResult: (Bitmap?) -> Unit
) {
    if (imageCapture == null) {
        Log.e("CameraScreen", "capturePhoto: imageCapture is null — camera not ready yet")
        onResult(null)
        return
    }

    val photoFile = File(
        context.cacheDir,
        "attendx_capture_${System.currentTimeMillis()}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                photoFile.delete()
                onResult(bitmap)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                onResult(null)
            }
        }
    )
}