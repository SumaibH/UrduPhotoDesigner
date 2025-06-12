package com.example.urduphotodesigner.common.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.model.CanvasSize
import com.example.urduphotodesigner.common.canvas.model.CanvasTemplate
import com.example.urduphotodesigner.common.canvas.model.ExportOptions
import com.example.urduphotodesigner.common.canvas.model.ExportResolution
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.sealed.BatchedCanvasAction
import com.example.urduphotodesigner.common.canvas.sealed.CanvasAction
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.domain.usecase.GetFontsUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Stack
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val getFontsUseCase: GetFontsUseCase,
) : ViewModel() {
    private val _canvasActions = Stack<CanvasAction>()
    private val _redoStack = Stack<CanvasAction>()
    private val _canvasElements = MutableLiveData<List<CanvasElement>>(emptyList())
    val canvasElements: MutableLiveData<List<CanvasElement>> = _canvasElements

    private val _exportOptions = MutableLiveData<ExportOptions>()
    val exportOptions: LiveData<ExportOptions> = _exportOptions

    private val _localFonts = MutableStateFlow<List<FontEntity>>(emptyList())
    private val localFonts: StateFlow<List<FontEntity>> = _localFonts.asStateFlow()

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

    private val _currentFont = MutableLiveData<FontEntity?>()
    val currentFont: LiveData<FontEntity?> = _currentFont

    private val _currentTextColor = MutableLiveData<Int>(Color.BLACK)
    val currentTextColor: LiveData<Int> = _currentTextColor

    private val _currentTextSize = MutableLiveData<Float>(40f)  // Initialize with a default size
    val currentTextSize: LiveData<Float> = _currentTextSize

    private val _currentTextAlignment = MutableLiveData<Paint.Align>(Paint.Align.CENTER)
    val currentTextAlignment: LiveData<Paint.Align> = _currentTextAlignment

    private val _currentTextOpacity = MutableLiveData<Int>(255)
    val currentTextOpacity: LiveData<Int> = _currentTextOpacity

    private val _canvasSize = MutableLiveData<CanvasSize>()
    val canvasSize: LiveData<CanvasSize> = _canvasSize

    private val _currentImageFilter = MutableLiveData<ImageFilter?>(null)
    val currentImageFilter: LiveData<ImageFilter?> = _currentImageFilter

    private val _currentTextBorderColor = MutableLiveData<Int>(Color.BLACK)
    val currentTextBorderColor: LiveData<Int> = _currentTextBorderColor

    private val _currentTextBorderWidth = MutableLiveData<Float>(1f)
    val currentTextBorderWidth: LiveData<Float> = _currentTextBorderWidth

    private val _currentTextShadowColor = MutableLiveData<Int>(Color.BLACK)
    val currentTextShadowColor: LiveData<Int> = _currentTextShadowColor

    private val _currentTextShadowDx = MutableLiveData<Float>(1f)
    val currentTextShadowDx: LiveData<Float> = _currentTextShadowDx

    private val _currentTextShadowDy = MutableLiveData<Float>(1f)
    val currentTextShadowDy: LiveData<Float> = _currentTextShadowDy

    private val _currentTextShadowRadius  = MutableLiveData<Float>(1f)
    val currentTextShadowRadius: LiveData<Float> = _currentTextShadowRadius

    private val _currentTextLabelColor = MutableLiveData<Int>(Color.BLACK)
    val currentTextLabelColor: LiveData<Int> = _currentTextLabelColor

    private val _currentTextLabelShape  = MutableLiveData<LabelShape>(LabelShape.RECTANGLE_FILL)
    val currentTextLabelShape: LiveData<LabelShape> = _currentTextLabelShape

    private var selectedElement: CanvasElement? = null
    private var currentBatchAction: BatchedCanvasAction? = null
    private var _isExplicitChange = false

    private val availableResolutions = listOf(
        ExportResolution(
            "Original Size",
            0,
            0,
            1f
        ), // Width and Height 0 indicate use canvas's current size
        ExportResolution("HD (1280x720)", 1280, 720),
        ExportResolution("Full HD (1920x1080)", 1920, 1080),
        ExportResolution("4K (3840x2160)", 3840, 2160),
        ExportResolution("Instagram Post (1080x1080)", 1080, 1080),
        ExportResolution("Instagram Story (1080x1920)", 1080, 1920)
    )

    val exportResolutions: List<ExportResolution> = availableResolutions

    init {
        observeLocalFonts()
        // Initialize exportOptions with a default value
        _exportOptions.value = ExportOptions(
            resolution = availableResolutions[0], // Default to "Original Size"
            quality = 100, // Default to High quality
            format = Bitmap.CompressFormat.PNG // Default to PNG format
        )
    }

    fun updateExportOptions(newOptions: ExportOptions) {
        _exportOptions.value = newOptions
    }

    /**
     * Sets the desired export resolution.
     * @param resolution The ExportResolution object to set.
     */
    fun setExportResolution(resolution: ExportResolution) {
        val currentOptions = _exportOptions.value ?: ExportOptions(availableResolutions[0])
        _exportOptions.value = currentOptions.copy(resolution = resolution)
    }

    /**
     * Sets the desired export quality (0-100).
     * @param quality The quality percentage.
     */
    fun setExportQuality(quality: Int) {
        val currentOptions = _exportOptions.value ?: ExportOptions(availableResolutions[0])
        _exportOptions.value = currentOptions.copy(quality = quality.coerceIn(0, 100))
    }

    /**
     * Sets the desired export format (PNG or JPEG).
     * @param format The Bitmap.CompressFormat to set.
     */
    fun setExportFormat(format: Bitmap.CompressFormat) {
        val currentOptions = _exportOptions.value ?: ExportOptions(availableResolutions[0])
        _exportOptions.value = currentOptions.copy(format = format)
    }

    private fun observeLocalFonts() {
        viewModelScope.launch {
            getFontsUseCase().collect { fonts ->
                _localFonts.value = fonts
                // After fonts are loaded, re-apply typeface to existing elements if any
                _canvasElements.value?.let { currentElements ->
                    _canvasElements.value = currentElements.map { element ->
                        // Create a copy to ensure its paint is re-initialized with context
                        val updatedElement = element.copy(context = element.context)
                        if (updatedElement.type == ElementType.TEXT && updatedElement.fontId != null) {
                            val font = fonts.find { it.id.toString() == updatedElement.fontId }
                            if (font != null && font.file_path?.isNotBlank() == true) {
                                try {
                                    updatedElement.paint.typeface =
                                        Typeface.createFromFile(font.file_path)
                                } catch (e: Exception) {
                                    println("Error re-applying typeface in observeLocalFonts: ${font.file_path}. Error: ${e.message}")
                                    updatedElement.paint.typeface = updatedElement.context?.let {
                                        ResourcesCompat.getFont(
                                            it, R.font.regular
                                        )
                                    } ?: Typeface.DEFAULT
                                }
                            } else {
                                updatedElement.paint.typeface = updatedElement.context?.let {
                                    ResourcesCompat.getFont(
                                        it, R.font.regular
                                    )
                                } ?: Typeface.DEFAULT
                            }
                        } else {
                            // Ensure non-text elements or text elements without fontId also have a default typeface if applicable
                            updatedElement.paint.typeface = updatedElement.context?.let {
                                ResourcesCompat.getFont(
                                    it, R.font.regular
                                )
                            } ?: Typeface.DEFAULT
                        }
                        updatedElement
                    }
                }
            }
        }
    }

    fun setCanvasSize(newSize: CanvasSize) {
        val oldSize = _canvasSize.value
        if (oldSize != newSize) {
            _canvasActions.push(
                CanvasAction.SetCanvasSize(
                    newSize, oldSize ?: newSize
                )
            ) // Push old size for undo
            _redoStack.clear()
            _canvasSize.value = newSize
            notifyUndoRedoChanged()
        }
    }

    fun endBatchUpdate(elementId: String) {
        val currentList = _canvasElements.value ?: emptyList()
        val finalElement = currentList.find { it.id == elementId }
            ?.copy(context = null, bitmap = null) // Capture final state for undo

        if (finalElement != null && currentBatchAction != null) {
            when (currentBatchAction) {
                is BatchedCanvasAction.DragBatch -> {
                    val initialElement =
                        (currentBatchAction as BatchedCanvasAction.DragBatch).initialElement
                    if (initialElement.x != finalElement.x || initialElement.y != finalElement.y) { // Only push if position changed
                        _canvasActions.push(
                            CanvasAction.UpdateElement(
                                elementId = elementId,
                                newElement = finalElement,
                                oldElement = initialElement
                            )
                        )
                    }
                }

                is BatchedCanvasAction.RotateBatch -> {
                    val initialElement =
                        (currentBatchAction as BatchedCanvasAction.RotateBatch).initialElement
                    if (initialElement.rotation != finalElement.rotation) { // Only push if rotation changed
                        _canvasActions.push(
                            CanvasAction.UpdateElement(
                                elementId = elementId,
                                newElement = finalElement,
                                oldElement = initialElement
                            )
                        )
                    }
                }

                is BatchedCanvasAction.ResizeBatch -> {
                    val initialElement =
                        (currentBatchAction as BatchedCanvasAction.ResizeBatch).initialElement
                    if (initialElement.scale != finalElement.scale) { // Only push if scale changed
                        _canvasActions.push(
                            CanvasAction.UpdateElement(
                                elementId = elementId,
                                newElement = finalElement,
                                oldElement = initialElement
                            )
                        )
                    }
                }
                // Add cases for other batch actions
                else -> { /* No specific batch action in progress */
                }
            }
            _redoStack.clear() // Clear redo stack on new action
            notifyUndoRedoChanged()
        }
        currentBatchAction = null // Clear the batch action
    }

    fun startBatchUpdate(elementId: String, actionType: String) {
        val currentList = _canvasElements.value ?: emptyList()
        val initialElement = currentList.find { it.id == elementId }
            ?.copy(context = null, bitmap = null) // Capture initial state for undo

        if (initialElement != null) {
            currentBatchAction = when (actionType) {
                "drag" -> BatchedCanvasAction.DragBatch(elementId, initialElement)
                "rotate" -> BatchedCanvasAction.RotateBatch(elementId, initialElement)
                "resize" -> BatchedCanvasAction.ResizeBatch(elementId, initialElement)
                else -> null
            }
        }
    }

    fun updateElement(updated: CanvasElement) {
        val currentList = _canvasElements.value ?: emptyList()
        val oldElement = currentList.find { it.id == updated.id }

        if (oldElement != null) {
            // Create a mutable copy to work with.
            // Pass the original context to ensure the paint's init block has it.
            val elementToUpdate = updated.copy(context = oldElement.context)

            // Explicitly re-apply the typeface if it's a TEXT element with a fontId
            if (elementToUpdate.type == ElementType.TEXT && elementToUpdate.fontId != null) {
                val font = localFonts.value.find { it.id.toString() == elementToUpdate.fontId }
                if (font != null && font.file_path?.isNotBlank() == true) {
                    elementToUpdate.paint.typeface = elementToUpdate.applyTypefaceFromFontList()
                } else {
                    // If font not found or path is blank, revert to default system font
                    elementToUpdate.paint.typeface =
                        elementToUpdate.context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                            ?: Typeface.DEFAULT
                }
            } else {
                // Ensure non-text elements or text elements without fontId also have a default typeface if applicable
                elementToUpdate.paint.typeface =
                    elementToUpdate.context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                        ?: Typeface.DEFAULT
            }


            // Replace the entire element with the updated version (now with the correct typeface)
            _canvasElements.value = currentList.map {
                if (it.id == elementToUpdate.id) elementToUpdate else it // Use elementToUpdate here
            }

            // Only push to undo stack if no batch action is in progress.
            // Continuous actions (drag, rotate, resize) will be handled by endBatchUpdate.
            if (currentBatchAction == null) {
                _canvasActions.push(
                    CanvasAction.UpdateElement(
                        elementId = elementToUpdate.id,
                        newElement = elementToUpdate.copy( // Copy for serialization, without transient data
                            context = null, bitmap = null
                        ),
                        oldElement = oldElement.copy( // Full copy without transient data
                            context = null, bitmap = null
                        )
                    )
                )
                _redoStack.clear()
                notifyUndoRedoChanged()
            }
        }
    }

    fun updateCanvasElementsOrderAndZIndex(reorderedList: List<CanvasElement>) {
        val oldList = _canvasElements.value ?: emptyList()

        // Create a new list with updated zIndex based on their position in the reorderedList
        val updatedList = reorderedList.mapIndexed { index, element ->
            // Pass context to ensure paint re-initialization can use it
            val copiedElement = element.copy(zIndex = index, context = element.context)
            // Re-apply font for text elements
            if (copiedElement.type == ElementType.TEXT && copiedElement.fontId != null) {
                val font = localFonts.value.find { it.id.toString() == copiedElement.fontId }
                if (font != null && font.file_path?.isNotBlank() == true) {
                    try {
                        copiedElement.paint.typeface = Typeface.createFromFile(font.file_path)
                    } catch (e: Exception) {
                        println("Error re-applying typeface in updateCanvasElementsOrderAndZIndex: ${font.file_path}. Error: ${e.message}")
                        copiedElement.paint.typeface = copiedElement.context?.let {
                            ResourcesCompat.getFont(
                                it, R.font.regular
                            )
                        } ?: Typeface.DEFAULT
                    }
                } else {
                    copiedElement.paint.typeface =
                        copiedElement.context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                            ?: Typeface.DEFAULT
                }
            } else {
                copiedElement.paint.typeface =
                    copiedElement.context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                        ?: Typeface.DEFAULT
            }
            copiedElement
        }

        _canvasActions.push(
            CanvasAction.UpdateCanvasElementsOrder(
                oldList.map {
                    it.copy(
                        context = null, bitmap = null
                    )
                }, // Store copies without transient data
                updatedList.map { it.copy(context = null, bitmap = null) })
        )
        _redoStack.clear()
        _canvasElements.value = updatedList
        notifyUndoRedoChanged()
    }

    fun setSelectedElement(element: CanvasElement?) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val context = currentList.firstOrNull()?.context // Get context from existing element

        // Deselect the previously selected element in the list
        currentList.find { it.isSelected }?.let { prevSelected ->
            if (prevSelected.id != element?.id) { // Only deselect if it's not the same element
                val index = currentList.indexOfFirst { it.id == prevSelected.id }
                if (index != -1) {
                    val deselectedElement = prevSelected.copy(isSelected = false, context = context)
                    // Re-apply font for deselected text elements
                    if (deselectedElement.type == ElementType.TEXT && deselectedElement.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == deselectedElement.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                deselectedElement.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface for deselected element: ${font.file_path}. Error: ${e.message}")
                                deselectedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            deselectedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else {
                        deselectedElement.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }
                    currentList[index] = deselectedElement
                }
            }
        }

        // Select the new element in the list and update current text properties
        if (element != null) {
            val index = currentList.indexOfFirst { it.id == element.id }
            if (index != -1) {
                val selectedElementCopy = element.copy(isSelected = true, context = context)
                // Re-apply font for selected text elements
                if (selectedElementCopy.type == ElementType.TEXT && selectedElementCopy.fontId != null) {
                    val font =
                        localFonts.value.find { font -> font.id.toString() == selectedElementCopy.fontId }
                    if (font != null && font.file_path?.isNotBlank() == true) {
                        try {
                            selectedElementCopy.paint.typeface =
                                Typeface.createFromFile(font.file_path)
                        } catch (e: Exception) {
                            println("Error re-applying typeface for selected element: ${font.file_path}. Error: ${e.message}")
                            selectedElementCopy.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else {
                        selectedElementCopy.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }
                } else {
                    selectedElementCopy.paint.typeface =
                        context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                            ?: Typeface.DEFAULT
                }
                currentList[index] = selectedElementCopy

                // Update all current text properties if the selected element is TEXT
                if (element.type == ElementType.TEXT) {
                    _currentFont.value = localFonts.value.find { font ->
                        selectedElementCopy.fontId != null && font.id.toString() == selectedElementCopy.fontId
                    }
                    _currentTextColor.value = selectedElementCopy.paintColor
                    _currentTextSize.value = selectedElementCopy.paintTextSize
                    _currentTextAlignment.value = selectedElementCopy.paintTextAlign
                    _currentTextOpacity.value = selectedElementCopy.paintAlpha
                    _currentImageFilter.value = null
                } else if (element.type == ElementType.IMAGE) {
                    _currentImageFilter.value = selectedElementCopy.imageFilter
                    // Clear text properties if image is selected
                    _currentFont.value = null
                    _currentTextColor.value = Color.BLACK
                    _currentTextSize.value = 40f
                    _currentTextAlignment.value = Paint.Align.CENTER
                    _currentTextOpacity.value = selectedElementCopy.paint.alpha
                }
            }
        } else {
            // If no element is selected, reset current text properties to defaults
            _currentFont.value = null
            _currentTextColor.value = Color.BLACK
            _currentTextSize.value = 40f
            _currentTextAlignment.value = Paint.Align.CENTER
            _currentTextOpacity.value = 255
            _currentImageFilter.value = null
        }

        this.selectedElement = element // Update the local reference
        _canvasElements.value = currentList // Emit the updated list to observers
    }

    fun onCanvasSelectionChanged(selectedListFromCanvas: List<CanvasElement>) {
        val currentElements = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val context = currentElements.firstOrNull()?.context // Get context from existing element

        // Create a set of IDs for the elements that are selected by the canvas
        val idsSelectedByCanvas = selectedListFromCanvas.map { it.id }.toSet()

        // Update the isSelected flag for all elements based on the canvas's selection
        val updatedList = currentElements.map { element ->
            val copiedElement = element.copy(
                isSelected = idsSelectedByCanvas.contains(element.id), context = context
            )
            // Re-apply font for text elements
            if (copiedElement.type == ElementType.TEXT && copiedElement.fontId != null) {
                copiedElement.paint.typeface = copiedElement.applyTypefaceFromFontList()
            } else {
                copiedElement.paint.typeface =
                    context?.let { ResourcesCompat.getFont(it, R.font.regular) } ?: Typeface.DEFAULT
            }
            copiedElement
        }
        _canvasElements.value =
            updatedList // Trigger update for SizedCanvasView and other observers

        // Find the first selected text element and update current text properties
        val firstSelectedTextElement =
            selectedListFromCanvas.firstOrNull { it.type == ElementType.TEXT }
        if (firstSelectedTextElement != null) {
            _currentFont.value = localFonts.value.find { font ->
                firstSelectedTextElement.fontId != null && font.id.toString() == firstSelectedTextElement.fontId
            }
            _currentTextColor.value = firstSelectedTextElement.paintColor
            _currentTextSize.value = firstSelectedTextElement.paintTextSize
            _currentTextAlignment.value = firstSelectedTextElement.paintTextAlign
            _currentTextOpacity.value = firstSelectedTextElement.paintAlpha
            _currentImageFilter.value = null
        } else {
            // If no text element is selected, reset current text properties to defaults
            _currentFont.value = null
            _currentTextColor.value = Color.BLACK
            _currentTextSize.value = 40f
            _currentTextAlignment.value = Paint.Align.CENTER
            _currentTextOpacity.value = 255
        }
        val firstSelectedImageElement =
            selectedListFromCanvas.firstOrNull { it.type == ElementType.IMAGE }
        if (firstSelectedImageElement != null) {
            _currentImageFilter.value = firstSelectedImageElement.imageFilter
        } else if (firstSelectedTextElement == null) { // Only clear if no image or text element is selected
            _currentImageFilter.value = null
        }
    }

    fun setSelectedElementsFromLayers(elementsToSelect: List<CanvasElement>) {
        val currentElements = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val context = currentElements.firstOrNull()?.context

        // Create a set of IDs for the elements that should be selected
        val idsToSelect = elementsToSelect.map { it.id }.toSet()

        // Update the isSelected flag for all elements based on the provided list
        val updatedList = currentElements.map { element ->
            val copiedElement =
                element.copy(isSelected = idsToSelect.contains(element.id), context = context)
            // Re-apply font for text elements
            if (copiedElement.type == ElementType.TEXT && copiedElement.fontId != null) {
                copiedElement.paint.typeface = copiedElement.applyTypefaceFromFontList()
            } else {
                copiedElement.paint.typeface =
                    context?.let { ResourcesCompat.getFont(it, R.font.regular) } ?: Typeface.DEFAULT
            }
            copiedElement
        }

        _canvasElements.value = updatedList // Emit the updated list to SizedCanvasView
    }

    fun getSelectedElement(): CanvasElement? {
        return _canvasElements.value?.find { it.isSelected } ?: selectedElement
    }

    fun setCanvasBackgroundColor(color: Int) {
        val previousColor = _backgroundColor.value ?: Color.WHITE
        if (color != previousColor) {
            _canvasActions.push(CanvasAction.SetBackgroundColor(color, previousColor))
            _redoStack.clear()
            _backgroundColor.value = color
            notifyUndoRedoChanged()
        }
    }

    fun setCanvasBackgroundImage(bitmap: Bitmap?) {
        val previousBitmap = _backgroundImage.value
        // Only push action if there's a change
        if (bitmap != previousBitmap) {
            _canvasActions.push(CanvasAction.SetBackgroundImage(bitmap, previousBitmap))
            _redoStack.clear()
            _backgroundImage.value = bitmap
            notifyUndoRedoChanged()
        }
    }

    fun setCanvasBackgroundGradient(colors: IntArray?, positions: FloatArray?) {
        val previousColors = _backgroundGradient.value?.first
        val previousPositions = _backgroundGradient.value?.second

        // Check if there's a significant change to warrant an undo action
        val colorsChanged = colors?.contentEquals(
            previousColors ?: intArrayOf()
        ) == false || (colors == null && previousColors != null) || (colors != null && previousColors == null)
        val positionsChanged = positions?.contentEquals(
            previousPositions ?: floatArrayOf()
        ) == false || (positions == null && previousPositions != null) || (positions != null && previousPositions == null)

        if (colorsChanged || positionsChanged) {
            _canvasActions.push(
                CanvasAction.SetBackgroundGradient(
                    colors ?: intArrayOf(), // Ensure not null for action
                    positions, previousColors, previousPositions
                )
            )
            _redoStack.clear()
            _backgroundGradient.value = Pair(colors, positions)
            notifyUndoRedoChanged()
        }
    }

    fun addSticker(bitmap: Bitmap?, context: Context) {
        val element = CanvasElement(
            context = context,
            type = ElementType.IMAGE,
            bitmap = bitmap,
            bitmapData = bitmap?.let { encodeBitmapToBase64(it) }, // Encode bitmap to Base64
            x = 150f,
            y = 150f,
            paintAlpha = 255 // Ensure initial opacity is set for serialization
        )
        // Ensure paint properties are set correctly after construction (including context)
        element.updatePaintProperties()

        _canvasActions.push(
            CanvasAction.AddSticker(
                element.copy(
                    context = null, bitmap = null
                )
            )
        ) // Push a copy for undo, without transient data
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
            paintColor = Color.BLACK,
            paintTextSize = 40f,
            paintTextAlign = Paint.Align.CENTER,
            paintAlpha = 255,
            fontId = null // Default font ID
        )
        // Ensure paint properties are set correctly after construction (including context)
        element.updatePaintProperties()
        // If a default font is desired on add, set it here.
        // For now, it will default to R.font.regular in CanvasElement's init if context is present.

        val action = CanvasAction.AddText(
            text, element.copy(context = null, bitmap = null)
        ) // Push a copy for undo, without transient data
        _canvasActions.push(action)
        _redoStack.clear()
        _canvasElements.value = (_canvasElements.value ?: emptyList()) + element
        selectedElement = element
        notifyUndoRedoChanged()
    }

    fun setFont(fontEntity: FontEntity, isExplicit: Boolean = true) {
        _isExplicitChange = isExplicit
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val context = currentList.firstOrNull()?.context
        val affectedElementsData = mutableListOf<Pair<String, String?>>()

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT && element.fontId != fontEntity.id.toString()) {
                affectedElementsData.add(element.id to element.fontId)
                element.copy(context = context).apply {
                    fontId = fontEntity.id.toString()
                    paint.typeface = try {
                        Typeface.createFromFile(fontEntity.file_path)
                    } catch (e: Exception) {
                        println("Error applying font: ${fontEntity.file_path}. Error: ${e.message}")
                        fontId = null
                        context?.let { ResourcesCompat.getFont(it, R.font.regular) } ?: Typeface.DEFAULT
                    }
                }
            } else element
        }

        if (affectedElementsData.isNotEmpty()) {
            val selectedTextElements = updatedList.filter { it.isSelected && it.type == ElementType.TEXT }
            _currentFont.value =
                when {
                    selectedTextElements.isEmpty() -> null
                    selectedTextElements.all { it.fontId == fontEntity.id.toString() } -> fontEntity
                    selectedTextElements.any { it.fontId == fontEntity.id.toString() } -> null // mixed
                    else -> fontEntity
                }

            _canvasElements.value = updatedList
            _canvasActions.push(CanvasAction.SetFont(fontEntity, affectedElementsData))
            _redoStack.clear()
            _isExplicitChange = false
            notifyUndoRedoChanged()
        }
    }

    fun setTextShadow(enabled: Boolean, color: Int, dx: Float, dy: Float, radius: Float) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                element.copy(
                    hasShadow = enabled,
                    shadowColor = color,
                    shadowDx = dx,
                    shadowDy = dy
                ).apply {
                    // Restore font!
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }
        _canvasElements.value = updatedList
        _currentTextShadowColor.value = color
        _currentTextShadowDx.value = dx
        _currentTextShadowDy.value = dy
        _currentTextShadowRadius.value = radius
    }

    fun setTextBorder(enabled: Boolean, color: Int, width: Float) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                element.copy(
                    hasBorder = enabled,
                    borderColor = color,
                    borderWidth = width
                ).apply {
                    // Restore font!
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }
        _canvasElements.value = updatedList
        _currentTextBorderColor.value = color
        _currentTextBorderWidth.value = width
    }

    fun setTextLabel(enabled: Boolean, color: Int, shape: LabelShape) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                element.copy(
                    hasLabel = enabled,
                    labelColor = color,
                    labelShape = shape
                ).apply {
                    // Restore font!
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }
        _canvasElements.value = updatedList
        _currentTextLabelColor.value = color
        _currentTextLabelShape.value = shape
    }


    private fun CanvasElement.applyTypefaceFromFontList(): Typeface {
        return fontId?.let { id ->
            localFonts.value.firstOrNull { it.id.toString() == id }?.file_path
                ?.takeIf { it.isNotBlank() }
                ?.let { Typeface.createFromFile(it) }
        } ?: context?.let { ResourcesCompat.getFont(it, R.font.regular) } ?: Typeface.DEFAULT
    }

    fun setTextColor(color: Int) {
        val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
        val context = currentList.firstOrNull()?.context
        var oldColor: Int? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                oldColor = oldColor ?: element.paintColor
                targetElementId = targetElementId ?: element.id

                element.copy(context = context).apply {
                    paintColor = color
                    paint.color = color

                    // Apply correct typeface
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }

        if (targetElementId != null) {
            _currentTextColor.value = color
            _canvasElements.value = updatedList
            _canvasActions.push(
                CanvasAction.SetTextColor(color, oldColor ?: Color.BLACK, targetElementId!!)
            )
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies text size to all currently selected text elements.
     */
    fun setTextSize(size: Float) {
        val currentList = _canvasElements.value ?: return
        val context = currentList.firstOrNull()?.context
        var oldSize: Float? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                oldSize = oldSize ?: element.paintTextSize
                targetElementId = targetElementId ?: element.id

                element.copy(context = context).apply {
                    paintTextSize = size
                    paint.textSize = size
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }

        if (targetElementId != null) {
            _currentTextSize.value = size
            _canvasElements.value = updatedList
            _canvasActions.push(CanvasAction.SetTextSize(size, oldSize ?: 40f, targetElementId!!))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies text alignment to all currently selected text elements.
     */
    fun setTextAlignment(alignment: Paint.Align) {
        val currentList = _canvasElements.value ?: return
        val context = currentList.firstOrNull()?.context
        var oldAlignment: Paint.Align? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected && element.type == ElementType.TEXT) {
                oldAlignment = oldAlignment ?: element.paintTextAlign
                targetElementId = targetElementId ?: element.id

                element.copy(context = context).apply {
                    paintTextAlign = alignment
                    paint.textAlign = alignment
                    paint.typeface = element.applyTypefaceFromFontList()
                }
            } else element
        }

        if (targetElementId != null) {
            _currentTextAlignment.value = alignment
            _canvasElements.value = updatedList
            _canvasActions.push(
                CanvasAction.SetTextAlignment(
                    alignment, oldAlignment ?: Paint.Align.CENTER, targetElementId!!
                )
            )
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    /**
     * Applies opacity to all currently selected elements.
     */
    fun setOpacity(opacity: Int) {
        val currentList = _canvasElements.value ?: return
        val context = currentList.firstOrNull()?.context
        var oldOpacity: Int? = null
        var targetElementId: String? = null

        val updatedList = currentList.map { element ->
            if (element.isSelected) {
                oldOpacity = oldOpacity ?: element.paintAlpha
                targetElementId = targetElementId ?: element.id

                element.copy(context = context).apply {
                    paintAlpha = opacity
                    paint.alpha = opacity
                    if (type == ElementType.TEXT) {
                        paint.typeface = element.applyTypefaceFromFontList()
                    }
                }
            } else element
        }

        if (targetElementId != null) {
            _currentTextOpacity.value = opacity
            _canvasElements.value = updatedList
            _canvasActions.push(CanvasAction.SetOpacity(opacity, oldOpacity ?: 255, targetElementId!!))
            _redoStack.clear()
            notifyUndoRedoChanged()
        }
    }

    fun updateText(element: CanvasElement) {
        val currentList = _canvasElements.value ?: return
        val textElement = currentList.find { it.id == element.id } ?: return
        val context = textElement.context
        val oldText = textElement.text

        val updatedElement = textElement.copy(text = element.text, context = context).apply {
            if (type == ElementType.TEXT) {
                paint.typeface = element.applyTypefaceFromFontList()
            }
        }

        _canvasElements.value = currentList.map { if (it.id == element.id) updatedElement else it }
        _canvasActions.push(
            CanvasAction.UpdateText(
                elementId = element.id, text = updatedElement.text, previousText = oldText
            )
        )
        _redoStack.clear()
        notifyUndoRedoChanged()
    }

    /**
     * Applies an image filter to the specified image element.
     * @param elementId The ID of the CanvasElement to apply the filter to.
     * @param newFilter The ImageFilter to apply.
     */
    fun applyImageFilter(elementId: String, newFilter: ImageFilter?, isExplicit: Boolean = true) {
        _isExplicitChange = isExplicit
        val currentList = _canvasElements.value ?: return
        val targetElement = currentList.find { it.id == elementId && it.type == ElementType.IMAGE } ?: return

        val oldFilter = targetElement.imageFilter
        if (oldFilter != newFilter) {
            val context = targetElement.context
            val updatedElement = targetElement.copy(imageFilter = newFilter, context = context)

            _canvasElements.value = currentList.map { if (it.id == updatedElement.id) updatedElement else it }
            if (updatedElement.isSelected) _currentImageFilter.value = newFilter
            _canvasActions.push(CanvasAction.ApplyImageFilter(elementId, newFilter, oldFilter))
            _redoStack.clear()
            _isExplicitChange = false
            notifyUndoRedoChanged()
        }
    }

    fun removeElement(element: CanvasElement) {
        val currentList = _canvasElements.value ?: emptyList()
        if (currentList.any { it.id == element.id }) { // Check by ID in case it's a copy
            _canvasActions.push(
                CanvasAction.RemoveElement(
                    element.copy(
                        context = null, bitmap = null
                    )
                )
            ) // Push a copy for undo, without transient data
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
        // Always try to get context from an existing element for re-applying paint properties
        val context = _canvasElements.value?.firstOrNull()?.context

        when (action) {
            is CanvasAction.UpdateElement -> {
                val updatedList = _canvasElements.value?.map {
                    if (it.id == action.elementId) {
                        val elementToApply = if (isRedo) action.newElement else action.oldElement
                        elementToApply.copy(
                            id = it.id, context = context
                        ).apply {
                            updatePaintProperties()
                        }

                        // Re-apply typeface for text elements after updating properties
                        if (elementToApply.type == ElementType.TEXT && elementToApply.fontId != null) {
                            val font =
                                localFonts.value.find { font -> font.id.toString() == elementToApply.fontId }
                            if (font != null && font.file_path?.isNotBlank() == true) {
                                try {
                                    elementToApply.paint.typeface =
                                        Typeface.createFromFile(font.file_path)
                                } catch (e: Exception) {
                                    println("Error re-applying typeface in undo/redo UpdateElement: ${font.file_path}. Error: ${e.message}")
                                    elementToApply.paint.typeface =
                                        context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                            ?: Typeface.DEFAULT
                                }
                            } else {
                                elementToApply.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else if (elementToApply.type == ElementType.TEXT) {
                            elementToApply.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }


                        // If it's an image, decode the bitmap data
                        if (elementToApply.type == ElementType.IMAGE && elementToApply.bitmapData != null) {
                            elementToApply.bitmap =
                                decodeBase64ToBitmap(elementToApply.bitmapData!!)
                        }
                        elementToApply
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
                        action.previousColors, action.previousPositions
                    )
            }

            is CanvasAction.AddSticker -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    action.sticker.context = context // Re-apply context
                    action.sticker.updatePaintProperties()
                    // Decode bitmap data for image elements
                    if (action.sticker.type == ElementType.IMAGE && action.sticker.bitmapData != null) {
                        action.sticker.bitmap = decodeBase64ToBitmap(action.sticker.bitmapData!!)
                    }
                    _canvasElements.value = currentList + action.sticker
                } else {
                    _canvasElements.value = currentList.filter { it.id != action.sticker.id }
                }
            }

            is CanvasAction.AddText -> {
                val currentList = _canvasElements.value ?: emptyList()
                if (isRedo) {
                    action.element.context = context // Re-apply context
                    action.element.updatePaintProperties()
                    // Re-apply typeface for text elements after updating properties
                    if (action.element.type == ElementType.TEXT && action.element.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == action.element.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                action.element.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo AddText: ${font.file_path}. Error: ${e.message}")
                                action.element.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            action.element.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (action.element.type == ElementType.TEXT) {
                        action.element.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    _canvasElements.value = currentList + action.element
                } else {
                    _canvasElements.value = currentList.filter { it.id != action.element.id }
                }
            }

            is CanvasAction.SetFont -> {
                val currentElements = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val updatedElements = currentElements.map { element ->
                    // Find the corresponding affected element data for this element's ID
                    val affectedData = action.affectedElements.find { it.first == element.id }
                    if (affectedData != null && element.type == ElementType.TEXT) {
                        val fontIdToApply =
                            if (isRedo) action.newFontEntity.id.toString() else affectedData.second

                        val fontToApply = fontIdToApply?.let { id ->
                            localFonts.value.find { it.id.toString() == id }
                        }

                        val copiedElement =
                            element.copy(context = context) // Copy and re-apply context
                        if (fontToApply != null && fontToApply.file_path?.isNotBlank() == true) {
                            try {
                                copiedElement.paint.typeface =
                                    Typeface.createFromFile(fontToApply.file_path)
                                copiedElement.fontId = fontToApply.id.toString()
                            } catch (e: Exception) {
                                println("Error re-applying typeface in SetFont action: ${fontToApply.file_path}. Error: ${e.message}")
                                copiedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                copiedElement.fontId = null
                            }
                        } else {
                            // If font not found or path is null/blank, set default font
                            copiedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                            copiedElement.fontId = null
                        }
                        copiedElement
                    } else {
                        element
                    }
                }
                _canvasElements.value = updatedElements
                // Update currentFont LiveData based on whether it's redo or undo
                _currentFont.value =
                    if (isRedo) action.newFontEntity else action.affectedElements.firstOrNull()?.second?.let { fontId ->
                        localFonts.value.find { it.id.toString() == fontId }
                    }
            }


            is CanvasAction.SetTextColor -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val targetElement = currentList.find { it.id == action.elementId }
                if (targetElement != null) {
                    val colorToApply = if (isRedo) action.color else action.previousColor
                    // Create a copy to trigger paint re-initialization with context, then modify
                    val updatedElement = targetElement.copy(context = context).apply {
                        paint.color = colorToApply
                        paintColor = colorToApply // Update serializable property
                    }
                    // Re-apply typeface for text elements after updating properties
                    if (updatedElement.type == ElementType.TEXT && updatedElement.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == updatedElement.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                updatedElement.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo SetTextColor: ${font.file_path}. Error: ${e.message}")
                                updatedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            updatedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (updatedElement.type == ElementType.TEXT) {
                        updatedElement.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    _currentTextColor.value = colorToApply
                    _canvasElements.value =
                        currentList.map { if (it.id == updatedElement.id) updatedElement else it } // Trigger redraw
                }
            }

            is CanvasAction.SetTextSize -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val targetElement = currentList.find { it.id == action.elementId }
                if (targetElement != null) {
                    val sizeToApply = if (isRedo) action.size else action.previousSize
                    // Create a copy to trigger paint re-initialization with context, then modify
                    val updatedElement = targetElement.copy(context = context).apply {
                        paint.textSize = sizeToApply
                        paintTextSize = sizeToApply // Update serializable property
                    }
                    // Re-apply typeface for text elements after updating properties
                    if (updatedElement.type == ElementType.TEXT && updatedElement.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == updatedElement.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                updatedElement.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo SetTextSize: ${font.file_path}. Error: ${e.message}")
                                updatedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            updatedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (updatedElement.type == ElementType.TEXT) {
                        updatedElement.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    _currentTextSize.value = sizeToApply
                    _canvasElements.value =
                        currentList.map { if (it.id == updatedElement.id) updatedElement else it } // Trigger redraw
                }
            }

            is CanvasAction.SetTextAlignment -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val targetElement = currentList.find { it.id == action.elementId }
                if (targetElement != null) {
                    val alignmentToApply =
                        if (isRedo) action.alignment else action.previousAlignment
                    // Create a copy to trigger paint re-initialization with context, then modify
                    val updatedElement = targetElement.copy(context = context).apply {
                        paint.textAlign = alignmentToApply
                        paintTextAlign = alignmentToApply // Update serializable property
                    }
                    // Re-apply typeface for text elements after updating properties
                    if (updatedElement.type == ElementType.TEXT && updatedElement.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == updatedElement.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                updatedElement.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo SetTextAlignment: ${font.file_path}. Error: ${e.message}")
                                updatedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            updatedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (updatedElement.type == ElementType.TEXT) {
                        updatedElement.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    _currentTextAlignment.value = alignmentToApply
                    _canvasElements.value =
                        currentList.map { if (it.id == updatedElement.id) updatedElement else it } // Trigger redraw
                }
            }

            is CanvasAction.SetOpacity -> {
                val currentList = _canvasElements.value?.toMutableList() ?: mutableListOf()
                val targetElement = currentList.find { it.id == action.elementId }
                if (targetElement != null) {
                    val opacityToApply = if (isRedo) action.opacity else action.previousOpacity
                    // Create a copy to trigger paint re-initialization with context, then modify
                    val updatedElement = targetElement.copy(context = context).apply {
                        paint.alpha = opacityToApply
                        paintAlpha = opacityToApply // Update serializable property
                    }
                    // Re-apply typeface for text elements if applicable
                    if (updatedElement.type == ElementType.TEXT && updatedElement.fontId != null) {
                        val font =
                            localFonts.value.find { it.id.toString() == updatedElement.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                updatedElement.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo SetOpacity: ${font.file_path}. Error: ${e.message}")
                                updatedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            updatedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (updatedElement.type == ElementType.TEXT) {
                        updatedElement.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    _currentTextOpacity.value = opacityToApply
                    _canvasElements.value =
                        currentList.map { if (it.id == updatedElement.id) updatedElement else it } // Trigger redraw
                }
            }

            is CanvasAction.UpdateText -> {
                val currentList = _canvasElements.value ?: emptyList()
                val updatedList = currentList.map { element ->
                    if (element.id == action.elementId) {
                        val copiedElement = element.copy(
                            text = if (isRedo) action.text else action.previousText,
                            context = context
                        )
                        // Re-apply typeface for text elements
                        if (copiedElement.type == ElementType.TEXT && copiedElement.fontId != null) {
                            val font =
                                localFonts.value.find { it.id.toString() == copiedElement.fontId }
                            if (font != null && font.file_path?.isNotBlank() == true) {
                                try {
                                    copiedElement.paint.typeface =
                                        Typeface.createFromFile(font.file_path)
                                } catch (e: Exception) {
                                    println("Error re-applying typeface in undo/redo UpdateText: ${font.file_path}. Error: ${e.message}")
                                    copiedElement.paint.typeface =
                                        context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                            ?: Typeface.DEFAULT
                                }
                            } else {
                                copiedElement.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else if (copiedElement.type == ElementType.TEXT) {
                            copiedElement.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                        copiedElement
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
                    // Re-apply context and paint properties
                    action.element.context = context
                    action.element.updatePaintProperties()
                    // Re-apply typeface for text elements
                    if (action.element.type == ElementType.TEXT && action.element.fontId != null) {
                        val font =
                            localFonts.value.find { font -> font.id.toString() == action.element.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                action.element.paint.typeface =
                                    Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo RemoveElement: ${font.file_path}. Error: ${e.message}")
                                action.element.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            action.element.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (action.element.type == ElementType.TEXT) {
                        action.element.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }

                    // Decode bitmap data for image elements
                    if (action.element.type == ElementType.IMAGE && action.element.bitmapData != null) {
                        action.element.bitmap = decodeBase64ToBitmap(action.element.bitmapData!!)
                    }
                    if (!currentList.any { it.id == action.element.id }) {
                        _canvasElements.value = currentList + action.element.copy()
                    }
                }
            }

            is CanvasAction.UpdateCanvasElementsOrder -> {
                val listToApply = if (isRedo) action.newList else action.oldList
                // Re-apply context and update paint properties for all elements in the list
                listToApply.forEach {
                    it.context = context
                    it.updatePaintProperties()
                    // Re-apply typeface for text elements
                    if (it.type == ElementType.TEXT && it.fontId != null) {
                        val font = localFonts.value.find { font -> font.id.toString() == it.fontId }
                        if (font != null && font.file_path?.isNotBlank() == true) {
                            try {
                                it.paint.typeface = Typeface.createFromFile(font.file_path)
                            } catch (e: Exception) {
                                println("Error re-applying typeface in undo/redo UpdateCanvasElementsOrder: ${font.file_path}. Error: ${e.message}")
                                it.paint.typeface =
                                    context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                        ?: Typeface.DEFAULT
                            }
                        } else {
                            it.paint.typeface =
                                context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                    ?: Typeface.DEFAULT
                        }
                    } else if (it.type == ElementType.TEXT) {
                        it.paint.typeface =
                            context?.let { ResourcesCompat.getFont(it, R.font.regular) }
                                ?: Typeface.DEFAULT
                    }
                    // Decode bitmap data for image elements
                    if (it.type == ElementType.IMAGE && it.bitmapData != null) {
                        it.bitmap = decodeBase64ToBitmap(it.bitmapData!!)
                    }
                }
                _canvasElements.value = listToApply
            }

            is CanvasAction.SetCanvasSize -> {
                _canvasSize.value = if (isRedo) action.newSize else action.oldSize
            }

            is CanvasAction.ApplyImageFilter -> {
                val currentList = _canvasElements.value ?: emptyList()
                _canvasElements.value = currentList.map {
                    if (it.id == action.elementId) {
                        it.copy(
                            imageFilter = if (isRedo) action.oldFilter else action.newFilter,
                            context = context
                        )
                    } else it
                }
                // Update the current image filter LiveData if it's the selected element
                _canvasElements.value?.find { it.id == action.elementId && it.isSelected }?.let {
                    _currentImageFilter.value = if (isRedo) action.oldFilter else action.newFilter
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

        notifyUndoRedoChanged()
    }

    // Helper function to encode Bitmap to Base64 String
    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Helper function to decode Base64 String to Bitmap
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    // Function to save the current canvas state as a template
    fun saveTemplate(): String {
        val elementsToSave = _canvasElements.value?.map { element ->
            // Create a copy without the transient context and bitmap for serialization
            // Ensure bitmapData is populated for image elements
            element.copy(
                context = null,
                bitmap = null,
                bitmapData = if (element.type == ElementType.IMAGE && element.bitmap != null) {
                    encodeBitmapToBase64(element.bitmap!!)
                } else null
            )
        } ?: emptyList()

        val template = CanvasTemplate(
            canvasElements = elementsToSave,
            canvasSize = _canvasSize.value ?: CanvasSize(
                "Default", 0, 0f, 0f
            ), // Provide a default or handle null
            backgroundColor = _backgroundColor.value ?: Color.WHITE,
            backgroundImage = _backgroundImage.value?.let { encodeBitmapToBase64(it) }, // Encode background image
            backgroundGradientColors = _backgroundGradient.value?.first,
            backgroundGradientPositions = _backgroundGradient.value?.second
        )
        return Gson().toJson(template)
    }

    // Function to load a template and restore the canvas state
    fun loadTemplate(templateJson: String, context: Context) {
        val template = Gson().fromJson(templateJson, CanvasTemplate::class.java)

        // Clear current state before loading
        clearCanvas()

        // Restore canvas size
        _canvasSize.value = template.canvasSize

        // Restore background properties
        _backgroundColor.value = template.backgroundColor
        _backgroundImage.value =
            template.backgroundImage?.let { decodeBase64ToBitmap(it) }

        val loadedElements = template.canvasElements.map { serializedElement ->
            val elementWithContext = serializedElement.copy(context = context)

            if (elementWithContext.type == ElementType.TEXT && elementWithContext.fontId != null) {
                val font = localFonts.value.find { it.id.toString() == elementWithContext.fontId }
                if (font != null && font.file_path?.isNotBlank() == true) {
                    try {
                        elementWithContext.paint.typeface = Typeface.createFromFile(font.file_path)
                    } catch (e: Exception) {
                        println("Error re-applying typeface during loadTemplate: ${font.file_path}. Error: ${e.message}")
                        elementWithContext.paint.typeface =
                            ResourcesCompat.getFont(context, R.font.regular)
                    }
                } else {
                    // Fallback to default font if the specific font is not found
                    elementWithContext.paint.typeface =
                        ResourcesCompat.getFont(context, R.font.regular)
                }
            } else if (elementWithContext.type == ElementType.TEXT) {
                elementWithContext.paint.typeface = ResourcesCompat.getFont(context, R.font.regular)
            }


            // Decode bitmap data for IMAGE elements
            if (elementWithContext.type == ElementType.IMAGE && serializedElement.bitmapData != null) {
                elementWithContext.bitmap = decodeBase64ToBitmap(serializedElement.bitmapData!!)
            }
            elementWithContext
        }
        _canvasElements.value = loadedElements
        _canvasActions.clear() // Clear undo/redo history for loaded templates
        _redoStack.clear()
        notifyUndoRedoChanged()
    }

    fun isExplicitChange(): Boolean {
        return _isExplicitChange
    }
}