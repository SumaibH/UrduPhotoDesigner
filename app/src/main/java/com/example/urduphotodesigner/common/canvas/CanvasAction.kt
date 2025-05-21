package com.example.urduphotodesigner.common.canvas

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface

sealed class CanvasAction {
    data class SetBackgroundColor(val color: Int, val previousColor: Int) : CanvasAction()
    data class SetBackgroundImage(val bitmap: Bitmap?, val previousBitmap: Bitmap?) : CanvasAction()
    data class AddSticker(val sticker: CanvasElement) : CanvasAction()
    data class AddText(val text: String, val element: CanvasElement) : CanvasAction()
    data class SetFont(val typeface: Typeface, val previousTypeface: Typeface?, val elementId: String) : CanvasAction()
    data class SetTextColor(val color: Int, val previousColor: Int, val elementId: String) : CanvasAction()
    data class SetTextSize(val size: Float, val previousSize: Float, val elementId: String) : CanvasAction()
    data class SetTextAlignment(val alignment: Paint.Align, val previousAlignment: Paint.Align, val elementId: String) : CanvasAction()
    data class UpdateElement(val elementId: String, val newElement: CanvasElement, val oldElement: CanvasElement) : CanvasAction()
    data class SetOpacity(val opacity: Int, val previousOpacity: Int, val elementId: String) : CanvasAction()
    data class UpdateText(val text: String, val previousText: String, val elementId: String) : CanvasAction()
    data class RemoveElement(val element: CanvasElement) : CanvasAction()
    data class UpdateCanvasElementsOrder(val oldList: List<CanvasElement>, val newList: List<CanvasElement>) : CanvasAction()
    data class SetBackgroundGradient(
        val colors: IntArray,
        val positions: FloatArray?,
        val previousColors: IntArray?,
        val previousPositions: FloatArray?
    ) : CanvasAction()
}