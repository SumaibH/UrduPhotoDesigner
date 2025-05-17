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

    // LiveData to hold the current background color.  Useful for observers.
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
        val updatedList = _canvasElements.value?.map {
            if (it.id == updated.id) updated else it
        } ?: listOf(updated)
        _canvasElements.value = updatedList
    }

    fun setSelectedElement(element: CanvasElement?) {
        selectedElement = element
    }

    fun getSelectedElement(): CanvasElement? {
        return selectedElement;
    }

    fun setCanvasBackgroundColor(color: Int) {
        val previousColor = _backgroundColor.value ?: Color.WHITE
        _canvasActions.push(CanvasAction.SetBackgroundColor(color, previousColor))
        _redoStack.clear()
        _backgroundColor.value = color
    }

    fun setCanvasBackgroundImage(bitmap: Bitmap?) {
        val previousBitmap = _backgroundImage.value
        _canvasActions.push(CanvasAction.SetBackgroundImage(bitmap, previousBitmap))
        _redoStack.clear()
        _backgroundImage.value = bitmap
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
    }

    fun setFont(typeface: Typeface) {
        val previousTypeface = selectedElement?.paint?.typeface
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.SetFont(typeface, previousTypeface))
            _redoStack.clear()
            selectedElement?.paint?.typeface = typeface
            _currentFont.value = typeface
        }

    }

    fun setTextColor(color: Int) {
        val previousColor = selectedElement?.paint?.color ?: Color.BLACK
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.SetTextColor(color, previousColor))
            _redoStack.clear()
            selectedElement?.paint?.color = color
            _currentTextColor.value = color
        }
    }

    fun setTextSize(size: Float) {
        val previousSize = selectedElement?.paint?.textSize ?: 40f
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.SetTextSize(size, previousSize))
            _redoStack.clear()
            selectedElement?.paint?.textSize = size
            _currentTextSize.value = size
        }
    }

    fun setTextAlignment(alignment: Paint.Align) {
        val previousAlignment = selectedElement?.paint?.textAlign ?: Paint.Align.CENTER
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.SetTextAlignment(alignment, previousAlignment))
            _redoStack.clear()
            selectedElement?.paint?.textAlign = alignment
            _currentTextAlignment.value = alignment
        }
    }

    fun setOpacity(opacity: Int) {
        val previousOpacity = selectedElement?.paint?.alpha ?: 255
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.SetOpacity(opacity, previousOpacity))
            _redoStack.clear()
            selectedElement?.paint?.alpha = opacity
            _currentTextOpacity.value = opacity
        }
    }

    fun updateText(text: String) {
        val previousText = selectedElement?.text ?: ""
        if (selectedElement != null) {
            _canvasActions.push(CanvasAction.UpdateText(text, previousText))
            _redoStack.clear()
            selectedElement?.text = text
        }
    }

    fun removeElement(element: CanvasElement) {
        _canvasActions.push(CanvasAction.RemoveElement(element))
        _redoStack.clear()
        val currentList = _canvasElements.value ?: emptyList()
        _canvasElements.value = currentList.filter { it != element }
        selectedElement = null
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
                if (selectedElement != null) {
                    selectedElement?.paint?.typeface =
                        if (isRedo) action.typeface else action.previousTypeface
                    _currentFont.value = selectedElement?.paint?.typeface
                }
            }

            is CanvasAction.SetTextColor -> {
                if (selectedElement != null) {
                    selectedElement?.paint?.color =
                        if (isRedo) action.color else action.previousColor
                    _currentTextColor.value = selectedElement?.paint?.color
                }
            }

            is CanvasAction.SetTextSize -> {
                if (selectedElement != null) {
                    selectedElement?.paint?.textSize =
                        if (isRedo) action.size else action.previousSize
                    _currentTextSize.value = selectedElement?.paint?.textSize
                }
            }

            is CanvasAction.SetTextAlignment -> {
                if (selectedElement != null) {
                    selectedElement?.paint?.textAlign =
                        if (isRedo) action.alignment else action.previousAlignment
                    _currentTextAlignment.value = selectedElement?.paint?.textAlign
                }
            }

            is CanvasAction.SetOpacity -> {
                if (selectedElement != null) {
                    selectedElement?.paint?.alpha =
                        if (isRedo) action.opacity else action.previousOpacity
                    _currentTextOpacity.value = selectedElement?.paint?.alpha
                }
            }

            is CanvasAction.UpdateText -> {
                if (selectedElement != null) {
                    selectedElement?.text = if (isRedo) action.text else action.previousText
                }
            }

            is CanvasAction.RemoveElement -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    _canvasElements.value = currentList.filter { it != action.element }
                } else {
                    _canvasElements.value = currentList + action.element
                }

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
        selectedElement = null

        notifyUndoRedoChanged()
    }
}