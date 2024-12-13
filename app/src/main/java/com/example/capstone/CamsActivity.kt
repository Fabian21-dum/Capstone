package com.example.capstone

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.capstone.HandLandmarkerHelper.Companion.TAG
import com.example.capstone.databinding.ActivityCamsBinding
import com.google.mediapipe.framework.MediaPipeException
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CamsActivity : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener,
    ASLTranslatorHelper.ASLTranslationListener {

    private lateinit var cameraExecutor: ExecutorService
    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    private lateinit var binding: ActivityCamsBinding
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private var isFrontCamera = true
    private lateinit var aslTranslatorHelper: ASLTranslatorHelper
    private var isProcessingFrame = false // Flag to avoid race conditions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overlayView = binding.overlayView
        previewView = binding.previewView

        // Initialize HandLandmarkerHelper and ASLTranslatorHelper
        setupHandLandmarker()
        aslTranslatorHelper = ASLTranslatorHelper(
            context = this,
            modelPath = "modelAll3.tflite",
            listener = this
        )

        // Initialize camera executor with a fixed thread pool (single-threaded processing)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Camera switch functionality
        binding.camera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        // Back button functionality
        binding.back.setOnClickListener {
            cleanupAndFinish()
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Create Preview use case
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Set up ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageFrame(imageProxy)
            }

            try {
                cameraProvider.unbindAll() // Unbind any previous use cases
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CamsActivity", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageFrame(imageProxy: ImageProxy) {
        if (isProcessingFrame || handLandmarkerHelper == null) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        try {
            handLandmarkerHelper?.detectLiveStream(imageProxy, isFrontCamera)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}", e)
        } finally {
            imageProxy.close()
            isProcessingFrame = false // Ensure the flag is reset
        }
    }

    private fun setupHandLandmarker() {
        if (handLandmarkerHelper == null) {
            Log.d(TAG, "Initializing HandLandmarker...")
            try {
                handLandmarkerHelper = HandLandmarkerHelper(
                    context = this,
                    minHandDetectionConfidence = 0.5f,
                    minHandTrackingConfidence = 0.5f,
                    minHandPresenceConfidence = 0.5f,
                    maxNumHands = 1,
                    currentDelegate = HandLandmarkerHelper.DELEGATE_CPU,
                    runningMode = RunningMode.LIVE_STREAM,
                    handLandmarkerHelperListener = this
                )
                Log.d(TAG, "HandLandmarker initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing HandLandmarker: ${e.message}")
            }
        }
    }

    private fun cleanupAndFinish() {
        try {
            handLandmarkerHelper?.clearHandLandmarker() // Cleanup HandLandmarker
        } catch (e: Exception) {
            Log.e("CamsActivity", "Error clearing HandLandmarker: ${e.message}")
        }

        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
            try {
                if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                cameraExecutor.shutdownNow()
            }
        }

        finish() // Finish the activity after cleanup
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            if (resultBundle.results.isNotEmpty()) {
                val handLandmarkerResult = resultBundle.results[0]
                val landmarks = handLandmarkerResult.landmarks().flatten()

                if (landmarks.isEmpty()) {
                    binding.translate.text = "No hand detected"
                    return@runOnUiThread
                }

                val translationResult = aslTranslatorHelper.translateLandmarksToASL(landmarks)
                binding.translate.text = translationResult.letter
                Log.d("ASLTranslator", "Translated letter: ${translationResult.letter}")
            } else {
                binding.translate.text = "No hand detected"
            }
        }
    }

    override fun onTranslationSuccess(letter: String, confidence: Float, inferenceTime: Long) {
        binding.translate.text = letter
        Log.d("ASLTranslator", "Translation succeeded: $letter")
    }

    override fun onTranslationError(errorMessage: String) {
        Toast.makeText(this, "Translation Error: $errorMessage", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        handLandmarkerHelper?.clearHandLandmarker()
    }

    override fun onResume() {
        super.onResume()
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor() // Reinitialize the executor
        }
    }

    override fun onBackPressed() {
        cleanupAndFinish() // Ensure cleanup on back press
    }
}
