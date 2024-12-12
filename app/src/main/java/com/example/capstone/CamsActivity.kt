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

class CamsActivity : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {

    private lateinit var cameraExecutor: ExecutorService
    private var handLandmarkerHelper: HandLandmarkerHelper? = null // Make it nullable
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

        // Initialize HandLandmarkerHelper
        setupHandLandmarker()


        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up camera switch
        binding.camera.setOnClickListener {
            isFrontCamera = !isFrontCamera // Toggle the camera
            startCamera() // Restart the camera with the new camera selector
        }

        // Set up back button to go back to the dashboard
        binding.back.setOnClickListener {
            // Perform necessary cleanup
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

            // Create a Preview use case
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()

            preview.setSurfaceProvider(binding.previewView.surfaceProvider)

            // Set up ImageAnalysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (handLandmarkerHelper != null) {
                    handLandmarkerHelper?.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = isFrontCamera
                    )
                } else {
                    Log.e(TAG, "HandLandmarkerHelper is not initialized")
                    imageProxy.close() // Ensure imageProxy is always closed to avoid memory leaks
                }
            }

            try {
                cameraProvider.unbindAll() // Unbind the previous use cases
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

    private fun cleanupAndFinish() {
        try {
            handLandmarkerHelper?.clearHandLandmarker() // Ensure HandLandmarker is cleared
        } catch (e: Exception) {
            Log.e("CamsActivity", "Error clearing HandLandmarker: ${e.message}")
        }

        // Shut down camera executor
        cameraExecutor.shutdown()
        finish() // Finish the activity after cleanup
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
                    currentDelegate = HandLandmarkerHelper.DELEGATE_CPU, // or DELEGATE_GPU
                    runningMode = RunningMode.LIVE_STREAM,
                    handLandmarkerHelperListener = this
                )
                Log.d(TAG, "HandLandmarker initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing HandLandmarker: ${e.message}")
            }
        }
    }

    private fun handleImageProxy(imageProxy: ImageProxy) {
        try {
            // Check if handLandmarkerHelper is not null, implying it's initialized
            if (handLandmarkerHelper != null) {
                handLandmarkerHelper?.detectLiveStream(imageProxy, isFrontCamera)
            } else {
                Log.e(TAG, "HandLandmarkerHelper is not properly initialized yet.")
            }
        } catch (e: MediaPipeException) {
            Log.e(TAG, "MediaPipeException while processing frame: ${e.message}", e)
        } finally {
            imageProxy.close() // Ensure the image is always closed
        }
    }


    override fun onPause() {
        super.onPause()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.execute { handLandmarkerHelper?.clearHandLandmarker() }
        } else {
            Log.w(TAG, "cameraExecutor is already shut down.")
        }
    }

    override fun onBackPressed() {
        cleanupAndFinish() // Ensure cleanup is done before finishing the activity
        super.onBackPressed()
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            if (resultBundle.results.isNotEmpty()) {
                for (handLandmarks in resultBundle.results) {
                    for (landmarkList in handLandmarks.landmarks()) {
                        overlayView.updateLandmarks(landmarkList)

                        // Translate landmarks to ASL
                        val inputArray = FloatArray(63) // Assuming 21 landmarks with 3 coordinates each (x, y, z)
                        landmarkList.forEachIndexed { index, landmark ->
                            inputArray[index * 3] = landmark.x()
                            inputArray[index * 3 + 1] = landmark.y()
                            inputArray[index * 3 + 2] = landmark.z()
                        }

//                        // Pass the inputArray to ASLTranslatorHelper for translation
//                        val translatedLetter = aslTranslatorHelper.translate(inputArray)
//
//                        // Update the TextView with the translated letter
//                        binding.translate.text = translatedLetter
                    }
                }
            } else {
                Log.d("CamsActivity", "No hands detected")
            }
        }
    }

    private fun drawLandmark(x: Float, y: Float) {
        val adjustedX = x * overlayView.width
        val adjustedY = y * overlayView.height

        // Create a NormalizedLandmark object
        val landmark = NormalizedLandmark.create(
            adjustedX / overlayView.width,  // Normalized x-coordinate (0 to 1)
            adjustedY / overlayView.height, // Normalized y-coordinate (0 to 1)
            0f // Assuming z is 0 as it's not provided
        )

        // Update landmarks in the overlay
        overlayView.updateLandmarks(listOf(landmark))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
            try {
                if (!cameraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.w(TAG, "cameraExecutor did not terminate in time; forcing shutdown.")
                    cameraExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while waiting for executor shutdown.", e)
                cameraExecutor.shutdownNow() // Force shutdown if interrupted
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
    }
}
