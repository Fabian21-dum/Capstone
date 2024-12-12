package com.example.capstone

import android.content.Context
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class ASLTranslatorHelper(
    private val context: Context,
    private val modelPath: String,
    private val listener: TranslationListener
) {
    private var tflite: Interpreter? = null

    init {
        try {
            // Load the TFLite model
            val model = FileUtil.loadMappedFile(context, modelPath)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            tflite = Interpreter(model, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite model", e)
            listener.onError("Failed to load model")
        }
    }

    fun translateLandmarksToASL(result: HandLandmarkerResult) {
        val handLandmarks = result.landmarks() // List<List<NormalizedLandmark>>

        if (handLandmarks.isEmpty() || handLandmarks[0].isEmpty()) {
            listener.onError("No hand landmarks detected")
            return
        }

        // Flatten landmark coordinates (x, y, z) into an input array
        val inputArray = FloatArray(63) // 21 landmarks * 3 coordinates
        handLandmarks[0].forEachIndexed { index, normalizedLandmark ->
            inputArray[index * 3] = normalizedLandmark.x() // x coordinate
            inputArray[index * 3 + 1] = normalizedLandmark.y() // y coordinate
            inputArray[index * 3 + 2] = normalizedLandmark.z() // z coordinate
        }

        // Prepare input tensor
        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, inputArray.size), // Model expects [1, 63]
            DataType.FLOAT32
        )
        inputBuffer.loadArray(inputArray)

        // Prepare output tensor
        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, 26), // Assuming model outputs 26 classes for A-Z
            DataType.FLOAT32
        )

        // Run inference
        val startTime = SystemClock.uptimeMillis()
        tflite?.run(inputBuffer.buffer, outputBuffer.buffer.rewind())
        val inferenceTime = SystemClock.uptimeMillis() - startTime

        // Get model output
        val outputArray = outputBuffer.floatArray
        val predictedIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
        val confidence = outputArray.getOrNull(predictedIndex) ?: 0.0f

        if (predictedIndex != -1) {
            val predictedLetter = ('A' + predictedIndex).toString()
            Log.d(TAG, "Predicted letter: $predictedLetter with confidence: $confidence")
            listener.onTranslationResult(predictedLetter)
        } else {
            listener.onError("Unable to recognize gesture")
        }
    }

    interface TranslationListener {
        fun onTranslationResult(result: String)
        fun onError(error: String)
    }

    companion object {
        private const val TAG = "ASLTranslatorHelper"
    }
}
