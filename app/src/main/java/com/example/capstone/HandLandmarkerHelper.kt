package com.example.capstone

import CSVHelper
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate

class HandLandmarkerHelper(
    var minHandDetectionConfidence: Float = DEFAULT_HAND_DETECTION_CONFIDENCE,
    var minHandTrackingConfidence: Float = DEFAULT_HAND_TRACKING_CONFIDENCE,
    var minHandPresenceConfidence: Float = DEFAULT_HAND_PRESENCE_CONFIDENCE,
    var maxNumHands: Int = DEFAULT_NUM_HANDS,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    val handLandmarkerHelperListener: LandmarkerListener
) {
    private var handLandmarker: HandLandmarker? = null
    private val listener = handLandmarkerHelperListener

    private val csvHelper = CSVHelper(context)

    init {
        setupHandLandmarker()
    }

    fun isClose(): Boolean {
        return handLandmarker == null
    }

    fun setupHandLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(minHandDetectionConfidence)
                .setMinTrackingConfidence(minHandTrackingConfidence)
                .setMinHandPresenceConfidence(minHandPresenceConfidence)
                .setNumHands(maxNumHands)
                .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            handLandmarkerHelperListener.onError(
                "Hand Landmarker failed to initialize. See error logs for details"
            )
            Log.e(TAG, "MediaPipe failed to load the task with error: " + e.message)
            e.printStackTrace()
        } catch (e: RuntimeException) {
            handLandmarkerHelperListener.onError(
                "Hand Landmarker failed to initialize. See error logs for details", GPU_ERROR
            )
            Log.e(TAG, "Image classifier failed to load model with error: " + e.message)
            e.printStackTrace()
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            Log.e(TAG, "detectLiveStream called with incorrect running mode: $runningMode")
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM"
            )
        }

        val frameTime = SystemClock.uptimeMillis()

        try {
            val bitmap = imageProxyToBitmap(imageProxy)

            // Ensure the imageProxy is closed properly
            try {
                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    if (isFrontCamera) {
                        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )

                Log.d(TAG, "Bitmap dimensions after rotation: ${rotatedBitmap.width}x${rotatedBitmap.height}")

                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                detectAsync(mpImage, frameTime)
            } finally {
                imageProxy.close()  // Make sure imageProxy is always closed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during detectLiveStream processing", e)
            try {
                imageProxy.close()  // Ensure closing in case of errors
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing imageProxy", closeException)
            }
        }
    }



    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        if (handLandmarker == null) {
            Log.e(TAG, "HandLandmarker is not initialized")
            return
        }
        try {
            handLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error during detectAsync", e)
        }
    }

    // This method handles the image result processing
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage while not using RunningMode.IMAGE"
            )
        }

        val startTime = SystemClock.uptimeMillis()
        val mpImage = BitmapImageBuilder(image).build()

        handLandmarker?.detect(mpImage)?.also { landmarkResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime

            // Log detected landmarks for debugging
            val landmarks = landmarkResult.landmarks()
            Log.d(TAG, "Detected landmarks: ${landmarks.size}")

            if (landmarks.isEmpty()) {
                Log.e(TAG, "No landmarks detected.")
            }

            // ASL Translation
            val aslTranslator = ASLTranslatorHelper(context, "model_asltflite_with_metadata.tflite", object : ASLTranslatorHelper.ASLTranslationListener {
                override fun onTranslationSuccess(letter: String, confidence: Float, inferenceTime: Long) {
                    // Log to CSV when a translation is successful
                    csvHelper.appendTranslationToCSV(letter, confidence, inferenceTime)

                    // Log translation details to console
                    Log.d(TAG, "Translated ASL letter: $letter, Confidence: $confidence, Inference Time: $inferenceTime ms")
                }

                override fun onTranslationError(errorMessage: String) {
                    Log.e(TAG, "Translation error: $errorMessage")
                }
            })

            // Flatten landmarks before passing to translator
            aslTranslator.translateLandmarksToASL(landmarks.flatten())

            return ResultBundle(
                listOf(landmarkResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        handLandmarkerHelperListener?.onError("Hand Landmarker failed to detect.")
        return null
    }

    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun returnLivestreamResult(
        result: HandLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        val landmarks = result.landmarks()

        // Log detected landmarks for debugging
        Log.d(TAG, "Landmarks detected in livestream: ${landmarks.size}")

        if (landmarks.isEmpty()) {
            Log.e(TAG, "No landmarks detected in livestream.")
        }

        // ASL Translation
        val aslTranslator = ASLTranslatorHelper(context, "modelAll3.tflite", object : ASLTranslatorHelper.ASLTranslationListener {
            override fun onTranslationSuccess(letter: String, confidence: Float, inferenceTime: Long) {
                // Log to CSV when a translation is successful
                csvHelper.appendTranslationToCSV(letter, confidence, inferenceTime)

                // Log translation details to console
                Log.d(TAG, "Translated ASL letter: $letter, Confidence: $confidence, Inference Time: $inferenceTime ms")
            }

            override fun onTranslationError(errorMessage: String) {
                Log.e(TAG, "Translation error: $errorMessage")
            }
        })

        // Flatten landmarks before passing to translator
        aslTranslator.translateLandmarksToASL(landmarks.flatten())

        handLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    companion object {
        const val TAG = "HandLandmarkerHelper"
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_HANDS = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    data class ResultBundle(
        val results: List<HandLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}


