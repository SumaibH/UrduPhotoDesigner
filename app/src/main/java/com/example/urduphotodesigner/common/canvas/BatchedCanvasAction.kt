package com.example.urduphotodesigner.common.canvas

sealed class BatchedCanvasAction {
    data class DragBatch(val elementId: String, val initialElement: CanvasElement) : BatchedCanvasAction()
    data class RotateBatch(val elementId: String, val initialElement: CanvasElement) : BatchedCanvasAction()
    data class ResizeBatch(val elementId: String, val initialElement: CanvasElement) : BatchedCanvasAction()
    // Add other batch types as needed (e.g., MultiTouchScaleRotateBatch)
}