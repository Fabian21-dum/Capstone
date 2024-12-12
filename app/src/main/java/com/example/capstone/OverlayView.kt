package com.example.capstone

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class OverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    private val landmarks = mutableListOf<NormalizedLandmark>()

    // Method to update landmarks
    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>) {
        landmarks.clear()
        landmarks.addAll(newLandmarks)
        invalidate() // Triggers a redraw of the view
    }

    // Method to draw the landmarks
    override fun onDraw(canvas: Canvas) {  // Removed the nullable type Canvas?
        super.onDraw(canvas)

        // Draw landmarks
        for (landmark in landmarks) {
            val x = landmark.x() * width // Scale to view width
            val y = landmark.y() * height // Scale to view height
            canvas.drawCircle(x, y, 10f, paint)
        }
    }
}
