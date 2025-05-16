package com.example.urduphotodesigner.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SizedCanvasView @JvmOverloads constructor(
    context: Context,
    private val canvasWidth: Int = 300,
    private val canvasHeight: Int = 300,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Calculate scale ratio to fit canvas within view
        val widthRatio = parentWidth.toFloat() / canvasWidth
        val heightRatio = parentHeight.toFloat() / canvasHeight

        // Always scale to fit inside parent, whether larger or smaller
        scale = minOf(widthRatio, heightRatio)

        // Let the view take up the entire available space from parent
        setMeasuredDimension(parentWidth, parentHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Compute the scaled dimensions of the canvas
        val scaledWidth = canvasWidth * scale
        val scaledHeight = canvasHeight * scale

        // Compute offsets to center the scaled canvas
        offsetX = (width - scaledWidth) / 2f
        offsetY = (height - scaledHeight) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // Draw the canvas content
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), backgroundPaint)

        canvas.restore()
    }
}