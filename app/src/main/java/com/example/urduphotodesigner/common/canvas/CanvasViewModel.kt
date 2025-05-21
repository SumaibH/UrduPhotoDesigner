package com.example.urduphotodesigner.common.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Stack
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor() : ViewModel() {
    private val _canvasActions = Stack<CanvasAction>()
    private val _redoStack = Stack<CanvasAction>()
    private val _canvasElements = MutableLiveData<List<CanvasElement>>(emptyList())
    val canvasElements: MutableLiveData<List<CanvasElement>> = _canvasElements

    private val _canUndo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> get() = _canUndo

    private val _canRedo = MutableLiveData(false)
    val canRedo: LiveData<Boolean> get() = _canRedo

    // LiveData to hold the current background color. Useful for observers.
    private val _backgroundColor =
        MutableLiveData<Int>(Color.WHITE) // Initialize with a default color
    val backgroundColor: LiveData<Int> = _backgroundColor

    private val _backgroundImage = MutableLiveData<Bitmap?>()
    val backgroundImage: LiveData<Bitmap?> = _backgroundImage

    private val _backgroundGradient = MutableLiveData<Pair<IntArray?, FloatArray?>?>()
    val backgroundGradient: LiveData<Pair<IntArray?, FloatArray?>?> = _backgroundGradient

    private val _currentFont = MutableLiveData<Typeface?>()
    val currentFont: LiveData<Typeface?> = _currentFont

    private val _currentTextColor = MutableLiveData<Int>(Color.BLACK)
    val currentTextColor: LiveData<Int> = _currentTextColor

    private val _currentTextSize = MutableLiveData<Float>(40f)  // Initialize with a default size
    val currentTextSize: LiveData<Float> = _currentTextSize

    private val _currentTextAlignment = MutableLiveData<Paint.Align>(Paint.Align.CENTER)
    val currentTextAlignment: LiveData<Paint.Align> = _currentTextAlignment

    private val _currentTextOpacity = MutableLiveData<Int>(255)
    val currentTextOpacity: LiveData<Int> = _currentTextOpacity

    private var selectedElement: CanvasElement? = null

    fun updateElement(updated: CanvasElement) {
        val currentList = _canvasElements.value ?: emptyList()
        val oldElement = currentList.find { it.id == updated.id }

        if (oldElement != null) {
            // Replace the entire element with the updated version
            _canvasElements.value = currentList.map {
                if (it.id == updated.id) updated else it
            }

            // Always push to undo stack (since any property could have changed)
            _canvasActions.push(
                CanvasAction.UpdateElement(
                    elementId = updated.id,
                    newElement = updated.copy(), // Full copy
                    oldElement = oldElement.copy() // Full copy
                )
            )
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    fun updateCanvasElementsOrderAndZIndex(reorderedList: List<CanvasElement>) {
        val oldList = _canvasElements.value ?: emptyList()

        // Create a new list with updated zIndex based on their position in the reorderedList
        val updatedList = reorderedList.mapIndexed { index, element ->
            element.copy(zIndex = index) // Assign zIndex based on the new order
        }

        _canvasElements.value = updatedList
        _canvasActions.push(CanvasAction.UpdateCanvasElementsOrder(oldList, updatedList))
        _redoStack.clear()
        notifyUndoRedoChanged()
    }

    fun setSelectedElement(element: CanvasElement?) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()

        // Deselect the previously selected element in the list
        currentList.find { it.isSelected }?.let { prevSelected ->
            if (prevSelected.id != element?.id) { // Only deselect if it's not the same element
                val index = currentList.indexOfFirst { it.id == prevSelected.id }
                if (index != -1) {
                    currentList[index] = prevSelected.copy(isSelected = false)
                }
            }
        }

        // Select the new element in the list and update current text properties
        if (element != null) {
            val index = currentList.indexOfFirst { it.id == element.id }
            if (index != -1) {
                currentList[index] = element.copy(isSelected = true)
                // Update all current text properties if the selected element is TEXT
                if (element.type == ElementType.TEXT) {
                    _currentFont.value = element.paint.typeface
                    _currentTextColor.value = element.paint.color
                    _currentTextSize.value = element.paint.textSize
                    _currentTextAlignment.value = element.paint.textAlign
                    _currentTextOpacity.value = element.paint.alpha
                }
            }
        } else {
            // If no element is selected, reset current text properties to defaults
            _currentFont.value = null
            _currentTextColor.value = Color.BLACK
            _currentTextSize.value = 40f
            _currentTextAlignment.value = Paint.Align.CENTER
            _currentTextOpacity.value = 255
        }

        this.selectedElement = element // Update the local reference
        _canvasElements.value = currentList // Emit the updated list to observers
    }

    fun onCanvasSelectionChanged(selectedListFromCanvas: List<CanvasElement>) {
        val currentElements = _canvasElements.value?.toMutableList() ?: mutableListOf()

        // Create a set of IDs for the elements that are selected by the canvas
        val idsSelectedByCanvas = selectedListFromCanvas.map { it.id }.toSet()

        // Update the isSelected flag for all elements based on the canvas's selection
        val updatedList = currentElements.map { element ->
            element.copy(isSelected = idsSelectedByCanvas.contains(element.id))
        }
        _canvasElements.value = updatedList // Trigger update for SizedCanvasView and other observers

        // Find the first selected text element and update current text properties
        val firstSelectedTextElement = selectedListFromCanvas.firstOrNull { it.type == ElementType.TEXT }
        if (firstSelectedTextElement != null) {
            _currentFont.value = firstSelectedTextElement.paint.typeface
            _currentTextColor.value = firstSelectedTextElement.paint.color
            _currentTextSize.value = firstSelectedTextElement.paint.textSize
            _currentTextAlignment.value = firstSelectedTextElement.paint.textAlign
            _currentTextOpacity.value = firstSelectedTextElement.paint.alpha
        } else {
            // If no text element is selected, reset current text properties to defaults
            _currentFont.value = null
            _currentTextColor.value = Color.BLACK
            _currentTextSize.value = 40f
            _currentTextAlignment.value = Paint.Align.CENTER
            _currentTextOpacity.value = 255
        }
    }

    fun setSelectedElementsFromLayers(elementsToSelect: List<CanvasElement>) {
        val currentElements = _canvasElements.value?.toMutableList() ?: mutableListOf()

        // Create a set of IDs for the elements that should be selected
        val idsToSelect = elementsToSelect.map { it.id }.toSet()

        // Update the isSelected flag for all elements based on the provided list
        val updatedList = currentElements.map { element ->
            element.copy(isSelected = idsToSelect.contains(element.id))
        }

        _canvasElements.value = updatedList // Emit the updated list to SizedCanvasView
    }

    fun getSelectedElement(): CanvasElement? {
        return _canvasElements.value?.find { it.isSelected } ?: selectedElement
    }

    fun setCanvasBackgroundColor(color: Int) {
        val previousColor = _backgroundColor.value ?: Color.WHITE
        _canvasActions.push(CanvasAction.SetBackgroundColor(color, previousColor))
        _redoStack.clear()
        _backgroundColor.value = color
        notifyUndoRedoChanged()
    }

    fun setCanvasBackgroundImage(bitmap: Bitmap?) {
        val previousBitmap = _backgroundImage.value
        _canvasActions.push(CanvasAction.SetBackgroundImage(bitmap, previousBitmap))
        _redoStack.clear()
        _backgroundImage.value = bitmap
        notifyUndoRedoChanged()
    }

    fun setCanvasBackgroundGradient(colors: IntArray?, positions: FloatArray?) {
        val previousColors = _backgroundGradient.value?.first
        val previousPositions = _backgroundGradient.value?.second
        _canvasActions.push(
            CanvasAction.SetBackgroundGradient(
                colors ?: intArrayOf(), // Ensure not null
                positions,
                previousColors,
                previousPositions
            )
        )
        _redoStack.clear()
        _backgroundGradient.value = Pair(colors, positions)
        notifyUndoRedoChanged()
    }

    fun addSticker(bitmap: Bitmap?, context: Context) {
        val element = CanvasElement(
            context = context,
            type = ElementType.IMAGE,
            bitmap = bitmap,
            x = 150f,
            y = 150f,
        )
        _canvasActions.push(CanvasAction.AddSticker(element))
        _redoStack.clear()
        val currentList = _canvasElements.value ?: emptyList()
        _canvasElements.value = currentList + element
        notifyUndoRedoChanged()
    }

    fun addText(text: String, context: Context) {
        val element = CanvasElement(
            context = context,
            type = ElementType.TEXT,
            text = text,
            x = 150f,
            y = 150f,
        )
        val action = CanvasAction.AddText(text, element)
        _canvasActions.push(action)
        _redoStack.clear()
        _canvasElements.value = (_canvasElements.value ?: emptyList()) + element
        selectedElement = element
        notifyUndoRedoChanged()
    }

    fun setFont(typeface: Typeface) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        var changed = false
        var oldTypeface: Typeface? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                if (!changed) {
                    oldTypeface = element.paint.typeface
                    targetElementId = element.id
                }
                changed = true
                element.copy().apply { paint.typeface = typeface }
            } else {
                element
            }
        }
        if (changed) {
            _currentFont.value = typeface // Update UI state
            _canvasElements.value = updatedList // Trigger observers to redraw
            // Add to undo stack with the captured elementId
            _canvasActions.push(CanvasAction.SetFont(typeface, oldTypeface, targetElementId!!))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    fun setTextColor(color: Int) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        var changed = false
        var oldColor: Int? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                if (!changed) {
                    oldColor = element.paint.color
                    targetElementId = element.id
                }
                changed = true
                element.copy().apply { paint.color = color }
            } else {
                element
            }
        }
        if (changed) {
            _currentTextColor.value = color // Update UI state
            _canvasElements.value = updatedList // Trigger observers to redraw
            _canvasActions.push(CanvasAction.SetTextColor(color, oldColor ?: Color.BLACK, targetElementId ?: ""))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies text size to all currently selected text elements.
     */
    fun setTextSize(size: Float) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        var changed = false
        var oldSize: Float? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                if (!changed) {
                    oldSize = element.paint.textSize
                    targetElementId = element.id
                }
                changed = true
                element.copy().apply { paint.textSize = size }
            } else {
                element
            }
        }
        if (changed) {
            _currentTextSize.value = size // Update UI state
            _canvasElements.value = updatedList // Trigger observers to redraw
            _canvasActions.push(CanvasAction.SetTextSize(size, oldSize ?: 40f, targetElementId ?: ""))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies text alignment to all currently selected text elements.
     */
    fun setTextAlignment(alignment: Paint.Align) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        var changed = false
        var oldAlignment: Paint.Align? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                if (!changed) {
                    oldAlignment = element.paint.textAlign
                    targetElementId = element.id
                }
                changed = true
                element.copy().apply { paint.textAlign = alignment }
            } else {
                element
            }
        }
        if (changed) {
            _currentTextAlignment.value = alignment // Update UI state
            _canvasElements.value = updatedList // Trigger observers to redraw
            _canvasActions.push(CanvasAction.SetTextAlignment(alignment, oldAlignment ?: Paint.Align.CENTER, targetElementId ?: ""))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies opacity to all currently selected elements.
     */
    fun setOpacity(opacity: Int) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        var changed = false
        var oldOpacity: Int? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected) {
                if (!changed) {
                    oldOpacity = element.paint.alpha
                    targetElementId = element.id
                }
                changed = true
                element.copy().apply { paint.alpha = opacity }
            } else {
                element
            }
        }
        if (changed) {
            _currentTextOpacity.value = opacity // Update UI state
            _canvasElements.value = updatedList // Trigger observers to redraw
            _canvasActions.push(CanvasAction.SetOpacity(opacity, oldOpacity ?: 255, targetElementId ?: ""))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    fun updateText(element: CanvasElement) {
        val currentList = _canvasElements.value ?: emptyList()
        val textElement = currentList.find { it.id == element.id } ?: return
        val oldElement = textElement.copy()
        val updatedElement = textElement.copy(text = element.text)

        _canvasElements.value = currentList.map {
            if (it.id == element.id) {
                updatedElement
            } else {
                it
            }
        }

        _canvasActions.push(
            CanvasAction.UpdateElement(
                elementId = element.id,
                newElement = updatedElement,
                oldElement = oldElement
            )
        )
        _redoStack.clear()
        notifyUndoRedoChanged()
    }

    fun removeElement(element: CanvasElement) {
        val currentList = _canvasElements.value ?: emptyList()
        if (currentList.contains(element)) {
            _canvasActions.push(CanvasAction.RemoveElement(element.copy()))
            _redoStack.clear()
            _canvasElements.value = currentList.filter { it.id != element.id }
            selectedElement = null
            notifyUndoRedoChanged()
        }
    }

    fun undo() {
        if (_canvasActions.isEmpty()) return
        val action = _canvasActions.pop()
        _redoStack.push(action)
        applyAction(action, isRedo = false)
        notifyUndoRedoChanged()
    }

    fun redo() {
        if (_redoStack.isEmpty()) return
        val action = _redoStack.pop()
        _canvasActions.push(action)
        applyAction(action, isRedo = true)
        notifyUndoRedoChanged()
    }

    private fun notifyUndoRedoChanged() {
        _canUndo.value = _canvasActions.isNotEmpty()
        _canRedo.value = _redoStack.isNotEmpty()
    }

    private fun applyAction(action: CanvasAction, isRedo: Boolean) {
        when (action) {
            is CanvasAction.UpdateElement -> {
                val updatedList = _canvasElements.value?.map {
                    if (it.id == action.elementId) {
                        if (isRedo) action.newElement else action.oldElement
                    } else it
                }
                _canvasElements.value = updatedList ?: emptyList()
            }

            is CanvasAction.SetBackgroundColor -> {
                _backgroundColor.value = if (isRedo) action.color else action.previousColor
            }

            is CanvasAction.SetBackgroundImage -> {
                _backgroundImage.value = if (isRedo) action.bitmap else action.previousBitmap
            }

            is CanvasAction.SetBackgroundGradient -> {
                _backgroundGradient.value =
                    if (isRedo) Pair(action.colors, action.positions) else Pair(
                        action.previousColors,
                        action.previousPositions
                    )
            }

            is CanvasAction.AddSticker -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    _canvasElements.value = currentList + action.sticker
                } else {
                    _canvasElements.value = currentList - action.sticker
                }
            }

            is CanvasAction.AddText -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    _canvasElements.value = currentList + action.element
                } else {
                    _canvasElements.value = currentList.filter { it != action.element }
                }
            }

            is CanvasAction.SetFont -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedList = currentList.map { element ->
                    // Apply to all selected text elements, regardless of the stored elementId
                    if (element.isSelected && element.type == ElementType.TEXT) {
                        element.copy().apply { paint.typeface = if (isRedo) action.typeface else action.previousTypeface }
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
                _currentFont.value = if (isRedo) action.typeface else action.previousTypeface
            }

            is CanvasAction.SetTextColor -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedList = currentList.map { element ->
                    // Apply to all selected text elements, regardless of the stored elementId
                    if (element.isSelected && element.type == ElementType.TEXT) {
                        element.copy().apply { paint.color = if (isRedo) action.color else action.previousColor }
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
                _currentTextColor.value = if (isRedo) action.color else action.previousColor
            }

            is CanvasAction.SetTextSize -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedList = currentList.map { element ->
                    // Apply to all selected text elements, regardless of the stored elementId
                    if (element.isSelected && element.type == ElementType.TEXT) {
                        element.copy().apply { paint.textSize = if (isRedo) action.size else action.previousSize }
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
                _currentTextSize.value = if (isRedo) action.size else action.previousSize
            }

            is CanvasAction.SetTextAlignment -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedList = currentList.map { element ->
                    // Apply to all selected text elements, regardless of the stored elementId
                    if (element.isSelected && element.type == ElementType.TEXT) {
                        element.copy().apply { paint.textAlign = if (isRedo) action.alignment else action.previousAlignment }
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
                _currentTextAlignment.value = if (isRedo) action.alignment else action.previousAlignment
            }

            is CanvasAction.SetOpacity -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedList = currentList.map { element ->
                    // Apply to all selected elements, regardless of the stored elementId
                    if (element.isSelected) {
                        element.copy().apply { paint.alpha = if (isRedo) action.opacity else action.previousOpacity }
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
                _currentTextOpacity.value = if (isRedo) action.opacity else action.previousOpacity
            }

            is CanvasAction.UpdateText -> {
                // This action is specifically for updating the text content of a single element.
                // It should use the elementId to target the correct element.
                val currentList = _canvasElements.value ?: emptyList()
                val updatedList = currentList.map { element ->
                    if (element.id == action.elementId) {
                        element.copy(text = if (isRedo) action.text else action.previousText)
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedList
            }

            is CanvasAction.RemoveElement -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    // For redo, remove the element (must compare by ID)
                    _canvasElements.value = currentList.filter { it.id != action.element.id }
                } else {
                    // For undo, add the element back (only if not already present)
                    if (!currentList.any { it.id == action.element.id }) {
                        _canvasElements.value = currentList + action.element.copy()
                    }
                }
            }
            is CanvasAction.UpdateCanvasElementsOrder -> {
                _canvasElements.value = if (isRedo) action.newList else action.oldList
            }
        }

        notifyUndoRedoChanged()
        //Always emit the current list
        _canvasElements.value = _canvasElements.value
    }

    fun clearCanvas() {
        _canvasActions.clear()
        _redoStack.clear()
        _canvasElements.value = emptyList()
        _backgroundColor.value = Color.WHITE
        _backgroundImage.value = null
        _backgroundGradient.value = null
        _currentFont.value = null
        _currentTextColor.value = Color.BLACK
        _currentTextSize.value = 40f
        _currentTextAlignment.value = Paint.Align.CENTER
        _currentTextOpacity.value = 255

        notifyUndoRedoChanged()
    }
}