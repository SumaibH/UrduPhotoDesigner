package com.example.urduphotodesigner.common.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import com.example.urduphotodesigner.common.views.SizedCanvasView
import com.example.urduphotodesigner.data.model.FontEntity

class CanvasManager(private val canvasView: SizedCanvasView) {
    fun setCanvasBackgroundColor(color: Int) { canvasView.setCanvasBackgroundColor(color) }
    fun setCanvasBackgroundImage(bitmap: Bitmap) { canvasView.setCanvasBackgroundImage(bitmap) }
    fun addSticker(bitmap: Bitmap, context: Context) { canvasView.addSticker(bitmap, context) }
    fun setCanvasBackgroundGradient(colors: IntArray, positions: FloatArray? = null) { canvasView.setCanvasBackgroundGradient(colors, positions) }
    fun addText(text: String, context: Context) { canvasView.addText(text, context) }
    fun setFont(fontEntity: FontEntity) { canvasView.setFont(fontEntity) }
    fun setTextColor(color: Int) { canvasView.setTextColor(color) }
    fun setTextSize(size: Float) { canvasView.setTextSize(size) }
    fun setTextAlignment(alignment: Paint.Align) { canvasView.setTextAlignment(alignment) }
    fun setOpacity(opacity: Int) { canvasView.setOpacity(opacity) }
    fun updateText(text: String) { canvasView.updateText(text) }
    fun removeSelectedElement() { canvasView.removeSelectedElement() }
    fun clearCanvas() { canvasView.clearCanvas() }
    fun setTextBorder(enable: Boolean, color: Int, width: Float) { canvasView.setTextBorder(enable, color, width) }
    fun setTextShadow(enable: Boolean, color: Int, dx: Float, dy: Float) { canvasView.setTextShadow(enable, color, dx, dy) }
    fun setTextLabel(enable: Boolean, color: Int, shape: LabelShape) { canvasView.setTextLabel(enable, color, shape) }
    fun syncElements(newElements: List<CanvasElement>) { canvasView.syncElements(newElements) }
    fun applyImageFilter(filter: ImageFilter?) {
        canvasView.applyImageFilter(filter)
    }
}
