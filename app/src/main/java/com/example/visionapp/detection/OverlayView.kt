/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.visionapp.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.visionapp.R
import com.example.visionapp.api.datatype.ResultAnnotation
import org.tensorflow.lite.examples.detection.tflite.Classifier
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var resultsOffline: ArrayList<Classifier.Recognition> = ArrayList<Classifier.Recognition>()
    private var resultsOnline: ArrayList<ResultAnnotation> = ArrayList<ResultAnnotation>()

    // default mode offline
    private var modelInUse: Int = 0

    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.yellow)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if(modelInUse == 0) {
            // mode offline
            for (result in resultsOffline) {
                val drawableRect = result.getLocation()

                // Draw bounding box around detected objects
                canvas.drawRect(drawableRect, boxPaint)

                // Create text to display alongside detected objects
                val drawableText = result.title.toString()

                // Draw rect behind display text
                textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
                val textWidth = bounds.width()
                val textHeight = bounds.height()

                val top = drawableRect.top
                val left = drawableRect.left

                canvas.drawRect(
                    left,
                    top,
                    left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                    top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                    textBackgroundPaint
                )

                // Draw text for detected object
                canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
            }
        } else {
            // mode online
            for (result in resultsOnline) {
                // TODO: draw bounding box
                val boundingBox = result.box


                val left = boundingBox[0] * scaleFactor
                val top = boundingBox[1] * scaleFactor

                val right = boundingBox[2] * scaleFactor
                val bottom = boundingBox[3] * scaleFactor

                // Draw bounding box around detected objects
                val drawableRect = RectF(left, top, right, bottom)
                canvas.drawRect(drawableRect, boxPaint)

                // Create text to display alongside detected objects
                val drawableText = result.label

                // Draw rect behind display text
                textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
                val textWidth = bounds.width()
                val textHeight = bounds.height()
                canvas.drawRect(
                    left,
                    top,
                    left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                    top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                    textBackgroundPaint
                )

                // Draw text for detected object
                canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
            }
        }
    }

    fun setResultsOffline(
      detectionResults: ArrayList<Classifier.Recognition>,
      mode: Int,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        resultsOffline = detectionResults
        modelInUse = mode

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    fun setResultsOnline(
        detectionResults: ArrayList<ResultAnnotation>,
        mode: Int,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        resultsOnline = detectionResults
        modelInUse = mode

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
