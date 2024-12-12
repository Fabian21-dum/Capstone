package com.example.capstone

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class ASLTranslatorHelper(context: Context, modelPath: String, val listener: ASLTranslationListener) {
    interface ASLTranslationListener {
        fun onTranslationSuccess(letter: String, confidence: Float, inferenceTime: Long)
        fun onTranslationError(errorMessage: String)
    }

    // Load TensorFlow Lite model
    private val tflite: Interpreter = try {
        Interpreter(FileUtil.loadMappedFile(context, modelPath))
    } catch (e: Exception) {
        Log.e(TAG, "Error loading TFLite model", e)
        throw RuntimeException("Failed to load model", e)
    }

    // Convert landmarks to an image input for the model
    fun prepareInputImageFromLandmarks(landmarks: List<NormalizedLandmark>): TensorBuffer {
        val flattenedLandmarks = mutableListOf<Float>()

        // Flatten each landmark's x, y, and z coordinates
        for (landmark in landmarks) {
            flattenedLandmarks.add(landmark.x())  // x-coordinate
            flattenedLandmarks.add(landmark.y())  // y-coordinate
            flattenedLandmarks.add(landmark.z())  // z-coordinate
        }

        // Check that we have exactly 63 features (21 landmarks * 3 coordinates)
        if (flattenedLandmarks.size != 63) {
            throw IllegalArgumentException("Expected 63 features but found ${flattenedLandmarks.size} features")
        }

        // Create a tensor of size (1, 63) for the input (batch size = 1)
        val inputArray = FloatArray(63) { i -> flattenedLandmarks[i] }

        // Create the input tensor with the correct size
        val inputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 63), DataType.FLOAT32)
        inputTensor.loadArray(inputArray)

        return inputTensor
    }

    fun translateLandmarksToASL(landmarks: List<NormalizedLandmark>): TranslationResult {
        if (landmarks.isEmpty()) {
            listener.onTranslationError("Empty landmarks list")
            return TranslationResult(letter = "?", confidence = 0f, inferenceTime = 0L)
        }

        val startTime = SystemClock.elapsedRealtime()  // Start timer for inference time

        return try {
            // Prepare the input tensor from the landmarks
            val inputTensor = prepareInputImageFromLandmarks(landmarks)

            // Create output tensor (size: 1, 28 - for 28 possible outputs)
            val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 28), DataType.FLOAT32)

            // Run inference
            tflite.run(inputTensor.buffer, outputTensor.buffer)

            val endTime = SystemClock.elapsedRealtime()  // End timer for inference time
            val inferenceTime = endTime - startTime  // Calculate inference time

            // Get the highest confidence from the output
            val confidence = outputTensor.floatArray.maxOrNull() ?: 0f

            // Find the index of the highest confidence
            val predictedIndex = outputTensor.floatArray.indexOfMax()

            // Map the index to a letter (0 -> "A", 1 -> "B", ..., 27 -> space or another symbol)
            val translatedLetter = when (predictedIndex) {
                in 0..25 -> ('A' + predictedIndex).toString()  // Map 0-25 to A-Z
                26 -> " "  // 26 could represent space (adjust this based on your model)
                else -> "?"  // Handle unknown indices
            }

            // Ensure this is run on the main thread if updating UI
            Handler(Looper.getMainLooper()).post {
                listener.onTranslationSuccess(translatedLetter, confidence, inferenceTime)
            }

            TranslationResult(letter = translatedLetter, confidence = confidence, inferenceTime = inferenceTime)
        } catch (e: Exception) {
            // Ensure error handling is done on the main thread
            Handler(Looper.getMainLooper()).post {
                listener.onTranslationError(e.message ?: "Unknown error")
            }
            throw e
        }
    }




    // Helper function to get the index of the maximum value in a FloatArray
    private fun FloatArray.indexOfMax(): Int {
        return this.indices.maxByOrNull { this[it] } ?: -1
    }

    // Data class for holding translation result
    data class TranslationResult(
        val letter: String,
        val confidence: Float,
        val inferenceTime: Long
    )

    companion object {
        private const val TAG = "ASLTranslatorHelper"
    }
}
