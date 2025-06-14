package com.example.urduphotodesigner.common.canvas.sealed

import android.graphics.Bitmap
import com.example.urduphotodesigner.common.canvas.enums.TextAlignment
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.model.CanvasSize
import com.example.urduphotodesigner.data.model.FontEntity

sealed class CanvasAction {
    data class UpdateElement(
        val elementId: String,
        val newElement: CanvasElement,
        val oldElement: CanvasElement
    ) : CanvasAction()

    data class SetBackgroundColor(val color: Int, val previousColor: Int) : CanvasAction()
    data class SetBackgroundImage(val bitmap: Bitmap?, val previousBitmap: Bitmap?) : CanvasAction()
    data class SetBackgroundGradient(
        val colors: IntArray,
        val positions: FloatArray?,
        val previousColors: IntArray?,
        val previousPositions: FloatArray?
    ) : CanvasAction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SetBackgroundGradient

            if (!colors.contentEquals(other.colors)) return false
            if (positions != null) {
                if (other.positions == null) return false
                if (!positions.contentEquals(other.positions)) return false
            } else if (other.positions != null) return false
            if (previousColors != null) {
                if (other.previousColors == null) return false
                if (!previousColors.contentEquals(other.previousColors)) return false
            } else if (other.previousColors != null) return false
            if (previousPositions != null) {
                if (other.previousPositions == null) return false
                if (!previousPositions.contentEquals(other.previousPositions)) return false
            } else if (other.previousPositions != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = colors.contentHashCode()
            result = 31 * result + (positions?.contentHashCode() ?: 0)
            result = 31 * result + (previousColors?.contentHashCode() ?: 0)
            result = 31 * result + (previousPositions?.contentHashCode() ?: 0)
            return result
        }
    }

    data class AddSticker(val sticker: CanvasElement) : CanvasAction()
    data class AddText(val text: String, val element: CanvasElement) : CanvasAction()

    // Modified SetFont to store a list of affected elements and their old font IDs
    data class SetFont(
        val newFontEntity: FontEntity,
        val affectedElements: List<Pair<String, String?>>
    ) : CanvasAction()

    data class SetTextColor(val color: Int, val previousColor: Int, val elementId: String) :
        CanvasAction()

    data class SetTextSize(val size: Float, val previousSize: Float, val elementId: String) :
        CanvasAction()

    data class SetTextAlignment(
        val alignment: TextAlignment,
        val previousAlignment: TextAlignment,
        val elementId: String
    ) : CanvasAction()

    data class SetOpacity(val opacity: Int, val previousOpacity: Int, val elementId: String) :
        CanvasAction()

    data class UpdateText(val elementId: String, val text: String, val previousText: String) :
        CanvasAction()

    data class RemoveElement(val element: CanvasElement) : CanvasAction()
    data class UpdateCanvasElementsOrder(
        val oldList: List<CanvasElement>,
        val newList: List<CanvasElement>
    ) : CanvasAction()

    data class SetCanvasSize(val newSize: CanvasSize, val oldSize: CanvasSize) : CanvasAction()

    data class ApplyImageFilter(
        val elementId: String,
        val newFilter: ImageFilter?,
        val oldFilter: ImageFilter?
    ) : CanvasAction()
}