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

        // Initialize camera executor with a fixed thread pool (ensure single thread processing)
        cameraExecutor = Executors.newFixedThreadPool(1)

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

            // Set up ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST to avoid buffer overflow
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keep only the latest frame
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    Log.d(TAG, "Acquiring ImageProxy: ${imageProxy.hashCode()}")
                    if (handLandmarkerHelper != null) {
                        // Perform hand detection on the image frame
                        handLandmarkerHelper?.detectLiveStream(imageProxy, isFrontCamera)
                    } else {
                        Log.e(TAG, "HandLandmarkerHelper is not initialized")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image: ${e.message}", e)
                } finally {
                    Log.d(TAG, "Closing ImageProxy: ${imageProxy.hashCode()}")
                    imageProxy.close() // Ensure that imageProxy is closed after processing
                }
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

    private fun handleImageProxy(imageProxy: ImageProxy) {
        try {
            // Process the image
            if (handLandmarkerHelper != null) {
                handLandmarkerHelper?.detectLiveStream(imageProxy, isFrontCamera)
            } else {
                Log.e(TAG, "HandLandmarkerHelper is not properly initialized yet.")
            }
        } catch (e: MediaPipeException) {
            Log.e(TAG, "Error processing frame: ${e.message}")
        } finally {
            // Always ensure the image is closed to prevent memory leaks
            imageProxy.close()
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
                    currentDelegate = HandLandmarkerHelper.DELEGATE_CPU, // Adjust as needed
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

        cameraExecutor.shutdown() // Shutdown the executor
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
                    Log.e(TAG, "No landmarks detected.")
                    binding.translate.text = "No hand detected"
                    return@runOnUiThread
                }

                val translationResult = aslTranslatorHelper.translateLandmarksToASL(landmarks)
                binding.translate.text = translationResult.letter
                Log.d("ASLTranslator", "Translated letter: ${translationResult.letter}")
            } else {
                Log.d("CamsActivity", "No hands detected")
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
        handLandmarkerHelper?.clearHandLandmarker() // Cleanup onPause
    }

    override fun onResume() {
        super.onResume()
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newFixedThreadPool(1) // Reinitialize executor on resume
        }
    }

    override fun onBackPressed() {
        cleanupAndFinish() // Ensure cleanup on back press
        super.onBackPressed()
    }
}
