package com.example.urduphotodesigner.common.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.urduphotodesigner.R
import java.util.UUID
import kotlin.random.Random

data class CanvasElement(
    val context: Context,
    var type: ElementType,
    var text: String = "",
    var bitmap: Bitmap? = null,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var paint: TextPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.regular)
        },
    val id: String = UUID.randomUUID().toString()
) {

        fun getBounds(): RectF {
            return if (type == ElementType.TEXT) {
                val bounds = Rect()
                paint.getTextBounds(text, 0, text.length, bounds)
                val w = bounds.width() * scale + 30
                val h = bounds.height() * scale + 30

                RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
            } else {
                val w = (bitmap?.width?.times(scale) ?: 0f) + 30
                val h = (bitmap?.height?.times(scale) ?: 0f) + 30
                RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
            }
        }

        fun getIconPositions(): Map<String, PointF> {
            val bounds = getBounds()
            val halfW = bounds.width() / 2
            val halfH = bounds.height() / 2
            return mapOf(
                "delete" to PointF(halfW, -halfH),
                "rotate" to PointF(halfW, halfH),
                "resize" to PointF(-halfW, halfH)
            )
        }
    }