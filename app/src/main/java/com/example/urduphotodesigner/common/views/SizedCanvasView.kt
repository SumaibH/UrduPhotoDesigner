package com.example.urduphotodesigner.common.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withTranslation
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.enums.BlendType
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.enums.LetterCasing
import com.example.urduphotodesigner.common.canvas.enums.ListStyle
import com.example.urduphotodesigner.common.canvas.enums.Mode
import com.example.urduphotodesigner.common.canvas.enums.TextAlignment
import com.example.urduphotodesigner.common.canvas.enums.TextDecoration
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.model.ExportOptions
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import com.example.urduphotodesigner.data.model.FontEntity
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

class SizedCanvasView @JvmOverloads constructor(
    context: Context,
    private val canvasWidth: Int = 300,
    private val canvasHeight: Int = 300,
    attrs: AttributeSet? = null,
    var onEditTextRequested: ((CanvasElement) -> Unit)? = null,
    var onElementChanged: ((CanvasElement) -> Unit)? = null,
    var onElementRemoved: ((CanvasElement) -> Unit)? = null,
    var onElementSelected: ((List<CanvasElement>) -> Unit)? = null,
    var onStartBatchUpdate: ((String, String) -> Unit)? = null,
    var onEndBatchUpdate: ((String) -> Unit)? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var gestureDetector: GestureDetector

    // Smaller icon size
    private var desiredIconScreenSizePx = 36f // Changed from 48f to make icons smaller
    private var iconTouched: String? = null

    private var backgroundGradient: LinearGradient? = null
    private var backgroundImage: Bitmap? = null
    private val canvasElements = mutableListOf<CanvasElement>()

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var currentMode: Mode = Mode.NONE

    // Store initial rotations for group rotation and single element rotation
    private var initialElementRotations = mutableMapOf<String, Float>()

    // Store initial positions of elements relative to the group's pivot at ACTION_DOWN
    private var initialElementPositionsRelativeToGroupPivot =
        mutableMapOf<String, Pair<Float, Float>>()
    private var initialAngle = 0f // For rotation calculation
    private var initialGroupPivotX = 0f // Store the group's pivot X at ACTION_DOWN
    private var initialGroupPivotY = 0f // Store the group's pivot Y at ACTION_DOWN

    private var initialPinchDistance = 0f
    private var initialPinchAngle = 0f
    private var initialScale = 1f
    private var initialRotation = 0f


    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    private val alignmentPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var showVerticalGuide = false
    private var showHorizontalGuide = false
    private var showRotationVerticalGuide = false // New guide for 0/180 degree rotation
    private var showRotationHorizontalGuide = false // New guide for 90/270 degree rotation


    private val removeIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_cross))
    }
    private val resizeIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_resize))
    }
    private val rotateIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_rotate))
    }
    private val editIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_edit_text))
    }

    // This list will contain references to elements from canvasElements that are currently selected.
    // We update the 'isSelected' boolean on the CanvasElement directly.
    private var selectedElements: CopyOnWriteArrayList<CanvasElement> = CopyOnWriteArrayList()
    private var lastTouchedElement: CanvasElement? =
        null // Keep track of the element that initiated the touch

    private fun drawableToBitmap(drawable: Drawable?): Bitmap {
        if (drawable == null) {
            return createBitmap(1, 1)
        }
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bmp = createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    /**
     * Syncs the canvas elements with a new list from the ViewModel.
     * Updates the internal `selectedElements` list based on the `isSelected` flag of incoming elements.
     */
    fun syncElements(newElements: List<CanvasElement>) {
        canvasElements.clear()
        canvasElements.addAll(newElements)
        selectedElements.clear()
        selectedElements.addAll(canvasElements.filter { it.isSelected }) // Rebuild selectedElements based on isSelected flags
        invalidate()
    }

    /**
     * Calculates the combined bounding box for all currently selected elements.
     * Returns an empty RectF if no elements are selected.
     */
    private fun getCombinedSelectedBounds(): RectF {
        if (selectedElements.isEmpty()) {
            return RectF()
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        val transformedPoints = mutableListOf<Float>()

        selectedElements.forEach { element ->
            val elementBounds = RectF(
                -element.getLocalContentWidth() / 2f,
                -element.getLocalContentHeight() / 2f,
                element.getLocalContentWidth() / 2f,
                element.getLocalContentHeight() / 2f
            )

            val matrix = Matrix()
            matrix.postRotate(element.rotation)
            matrix.postScale(element.scale, element.scale)
            matrix.postTranslate(element.x, element.y)

            val corners = floatArrayOf(
                elementBounds.left, elementBounds.top,
                elementBounds.right, elementBounds.top,
                elementBounds.right, elementBounds.bottom,
                elementBounds.left, elementBounds.bottom
            )
            matrix.mapPoints(corners)
            transformedPoints.addAll(corners.toList())
        }

        // Find min/max from all transformed corner points
        minX = transformedPoints.filterIndexed { index, _ -> index % 2 == 0 }.minOrNull() ?: minX
        minY = transformedPoints.filterIndexed { index, _ -> index % 2 != 0 }.minOrNull() ?: minY
        maxX = transformedPoints.filterIndexed { index, _ -> index % 2 == 0 }.maxOrNull() ?: maxX
        maxY = transformedPoints.filterIndexed { index, _ -> index % 2 != 0 }.maxOrNull() ?: maxY

        return RectF(minX, minY, maxX, maxY)
    }

    fun setLineSpacing(multiplier: Float) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.lineSpacing = multiplier
            element.updatePaintProperties()  // Ensure paint is updated after spacing change
            onElementChanged?.invoke(element)  // Notify listeners of the change
        }
        invalidate()  // Redraw the canvas
    }

    // Set letter spacing for selected text elements
    fun setLetterSpacing(spacing: Float) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.letterSpacing = spacing
            element.updatePaintProperties()  // Ensure paint is updated after spacing change
            onElementChanged?.invoke(element)  // Notify listeners of the change
        }
        invalidate()  // Redraw the canvas
    }

    // Set letter casing for selected text elements
    fun setLetterCasing(casing: LetterCasing) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.letterCasing = casing
            // Apply the appropriate letter casing
            element.text = when (casing) {
                LetterCasing.ALL_CAPS -> element.text.uppercase()
                LetterCasing.LOWER_CASE -> element.text.lowercase()
                LetterCasing.TITLE_CASE -> element.text.split(" ").joinToString(" ") {
                    it.capitalize(
                        Locale.ROOT
                    )
                }

                else -> element.text
            }
            onElementChanged?.invoke(element)  // Notify listeners of the change
        }
        invalidate()  // Redraw the canvas
    }

    fun setTextDecoration(decorations: Set<TextDecoration>) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.textDecoration = decorations
            element.updatePaintProperties()
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    // Set text alignment for selected text elements
    fun setTextAlignment(alignment: TextAlignment) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.alignment = alignment
            element.updatePaintProperties()  // Ensure paint is updated after alignment change
            onElementChanged?.invoke(element)  // Notify listeners of the change
        }
        invalidate()  // Redraw the canvas
    }

    // Set list style (bulleted or numbered) for selected text elements
    fun setListStyle(style: ListStyle) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.listStyle = style
            onElementChanged?.invoke(element)  // Notify listeners of the change
        }
        invalidate()  // Redraw the canvas
    }

    fun clearCanvas() {
        canvasElements.clear()
        selectedElements.clear()
        backgroundPaint.color = Color.WHITE // Or your default background color
        backgroundGradient = null
        backgroundImage = null
        invalidate()
    }

    fun setTextBorder(enabled: Boolean, color: Int = Color.BLACK, width: Float = 2f) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.hasStroke = enabled
            element.strokeColor = color
            element.strokeWidth = width
            element.updatePaintProperties()
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun removeTextBorder() = setTextBorder(false)

    fun setTextShadow(enabled: Boolean, color: Int = Color.GRAY, dx: Float = 1f, dy: Float = 1f) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.hasShadow = enabled
            element.shadowColor = color
            element.shadowDx = dx
            element.shadowDy = dy
            element.updatePaintProperties()
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun removeTextShadow() = setTextShadow(false)

    fun setTextLabel(
        enabled: Boolean,
        color: Int = Color.YELLOW,
        shape: LabelShape = LabelShape.RECTANGLE_FILL
    ) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.hasLabel = enabled
            element.labelColor = color
            element.labelShape = shape
            // If label rendering is in your onDraw, call invalidate
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun removeTextLabel() = setTextLabel(false)

    fun addText(text: String, context: Context) {
        val element = CanvasElement(
            context = context,
            type = ElementType.TEXT,
            text = text,
            x = canvasWidth / 2f,
            y = canvasHeight / 2f
        ).apply {
            // Assign a new zIndex (top of stack)
            zIndex = (canvasElements.maxByOrNull { it.zIndex }?.zIndex ?: 0) + 1
        }
        // Deselect all existing elements before adding and selecting the new one
        canvasElements.forEach { it.isSelected = false }
        selectedElements.clear() // Clear internal selected list as well
        element.isSelected = true // Select the new element
        canvasElements.add(element)
        selectedElements.add(element) // Add to internal selected list
        onElementSelected?.invoke(selectedElements) // Notify ViewModel of the new single selection
        invalidate()
    }

    fun removeSelectedElement() {
        // Remove all selected elements
        val elementsToRemove =
            selectedElements.toList() // Create a copy to avoid concurrent modification
        elementsToRemove.forEach { element ->
            canvasElements.remove(element)
            onElementRemoved?.invoke(element) // Notify ViewModel to remove for each
        }
        selectedElements.clear() // Clear the selected elements list
        invalidate()
    }

    fun applyImageFilter(filter: ImageFilter?) {
        val elementsToFilter =
            selectedElements.toList() // Create a copy to avoid concurrent modification
        elementsToFilter.forEach { element ->
            if (element != null && element.type == ElementType.IMAGE) {
                element.imageFilter = filter
                onElementChanged?.invoke(element) // Notify ViewModel of change
                invalidate()
            }
        }
    }

    fun setFont(fontEntity: FontEntity) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.fontId = fontEntity.id.toString()

            // Check if the file_path is not blank before attempting to create a typeface
            if (fontEntity.file_path?.isNotBlank()!!) {
                try {
                    element.paint.typeface = Typeface.createFromFile(fontEntity.file_path)
                } catch (e: Exception) {
                    // Handle potential errors if the file path is valid but the file itself is corrupt or unreadable
                    // You might log the error or set a default typeface here if needed
                    println("Error loading typeface from file: ${fontEntity.file_path}. Error: ${e.message}")

                    element.paint.typeface = Typeface.DEFAULT
                }
            } else {
                // If file_path is blank, do not set the typeface.
                // The existing typeface on the element will remain, or you could explicitly
                // set it to a default system typeface if that's desired when no custom font is selected.
                // For example:
                // element.paint.typeface = Typeface.DEFAULT
            }

            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun setTextColor(color: Int) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.paint.color = color
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun setTextSize(size: Float) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.paint.textSize = size
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun setOpacity(opacity: Int) {
        selectedElements.forEach { element ->
            element.paint.alpha = opacity
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun updateText(text: String) {
        selectedElements.firstOrNull { it.type == ElementType.TEXT }?.let { element ->
            element.text = text
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun setCanvasBackgroundColor(color: Int) {
        backgroundPaint.color = color
        backgroundGradient = null
        backgroundImage = null
        invalidate()
    }

    fun setCanvasBackgroundGradient(colors: IntArray, positions: FloatArray? = null) {
        backgroundGradient = LinearGradient(
            0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(),
            colors, positions, Shader.TileMode.CLAMP
        )
        backgroundImage = null
        invalidate()
    }

    fun setCanvasBackgroundImage(bitmap: Bitmap) {
        // Create a new bitmap with canvas dimensions
        val resultBitmap = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(resultBitmap)

        // Calculate scale to maintain aspect ratio while filling canvas
        val scale = max(
            canvasWidth.toFloat() / bitmap.width,
            canvasHeight.toFloat() / bitmap.height
        )

        // Create scaled version of source bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )

        // Calculate position to center the scaled bitmap
        val left = (canvasWidth - scaledBitmap.width) / 2f
        val top = (canvasHeight - scaledBitmap.height) / 2f

        // Draw the scaled bitmap onto our result bitmap
        canvas.drawBitmap(scaledBitmap, left, top, null)

        // Set as background
        backgroundImage = resultBitmap
        backgroundGradient = null
        invalidate()
    }

    /** Apply a horizontal fill gradient across the text bounds */
    fun setTextFillGradient(colors: IntArray, positions: FloatArray? = null) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.fillGradientColors = colors
            element.fillGradientPositions = positions
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    /** Apply a horizontal stroke gradient across the text bounds */
    fun setTextStrokeGradient(colors: IntArray, positions: FloatArray? = null) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.strokeGradientColors = colors
            element.strokeGradientPositions = positions
            onElementChanged?.invoke(element)
        }
        invalidate()
    }

    fun addSticker(bitmap: Bitmap, context: Context) {
        val canvasCenterX = canvasWidth / 2f
        val canvasCenterY = canvasHeight / 2f

        val widthRatio = canvasWidth.toFloat() / bitmap.width
        val heightRatio = canvasHeight.toFloat() / bitmap.height
        val minScale = minOf(1f, widthRatio, heightRatio)

        val element = CanvasElement(
            context = context,
            type = ElementType.IMAGE,
            bitmap = bitmap,
            x = canvasCenterX,
            y = canvasCenterY,
            scale = minScale
        )
        element.zIndex = (canvasElements.maxByOrNull { it.zIndex }?.zIndex ?: 0) + 1
        // Deselect all existing elements before adding and selecting the new one
        canvasElements.forEach { it.isSelected = false }
        selectedElements.clear() // Clear internal selected list as well
        element.isSelected = true // Select the new element
        canvasElements.add(element)
        selectedElements.add(element) // Add to internal selected list
        onElementSelected?.invoke(selectedElements) // Notify ViewModel of the new single selection
        invalidate()
    }

    /**
     * Exports the current canvas content to a Bitmap with specified resolution and quality.
     * @param options The export options including resolution, quality, and format.
     * @return A Bitmap representing the exported canvas.
     */
    fun exportCanvasToBitmap(options: ExportOptions): Bitmap {
        val outputWidth: Int
        val outputHeight: Int
        if (options.resolution.width == 0 && options.resolution.height == 0) {
            outputWidth = width
            outputHeight = height
        } else {
            outputWidth = options.resolution.width
            outputHeight = options.resolution.height
        }

        val outputBitmap = createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(outputBitmap)

        // Draw background
        outputCanvas.drawRect(
            0f,
            0f,
            outputWidth.toFloat(),
            outputHeight.toFloat(),
            backgroundPaint
        )

        // Draw background gradient if exists
        backgroundGradient?.let {
            val gradientMatrix = Matrix()
            // Scale gradient to fill the output canvas dimensions
            gradientMatrix.setScale(
                outputWidth.toFloat() / canvasWidth,
                outputHeight.toFloat() / canvasHeight
            )
            it.setLocalMatrix(gradientMatrix)
            outputCanvas.drawRect(
                0f,
                0f,
                outputWidth.toFloat(),
                outputHeight.toFloat(),
                Paint().apply { shader = it })
        }

        // Draw background image if exists
        backgroundImage?.let {
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val srcRect = Rect(0, 0, it.width, it.height)
            val destRect = Rect(0, 0, outputWidth, outputHeight)
            outputCanvas.drawBitmap(it, srcRect, destRect, imagePaint)
        }

        // Calculate scaling factor for elements
        val scaleFactorX = outputWidth.toFloat() / canvasWidth
        val scaleFactorY = outputHeight.toFloat() / canvasHeight

        // Draw elements to the output canvas
        canvasElements.sortedBy { it.zIndex }.forEach { element ->
            val elementMatrix = Matrix()
            // Translate to element's scaled position
            elementMatrix.postTranslate(element.x * scaleFactorX, element.y * scaleFactorY)
            // Apply element's rotation
            elementMatrix.postRotate(element.rotation)
            // Apply element's scale (combined with canvas scale)
            elementMatrix.postScale(element.scale * scaleFactorX, element.scale * scaleFactorY)


            outputCanvas.withMatrix(elementMatrix) {
                val elementPaint = Paint(element.paint) // Create a copy of the element's paint

                // Apply filter to the elementPaint if it's an image
                if (element.type == ElementType.IMAGE && element.bitmap != null) {
                    elementPaint.colorFilter = when (element.imageFilter) {
                        ImageFilter.Grayscale -> ColorMatrixColorFilter(ColorMatrix().apply {
                            setSaturation(0f)
                        })

                        ImageFilter.Sepia -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    0.393f, 0.769f, 0.189f, 0f, 0f,
                                    0.349f, 0.686f, 0.168f, 0f, 0f,
                                    0.272f, 0.534f, 0.131f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.Invert -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.CoolTint -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    1.1f, 0f, 0f, 0f, -20f,  // Red decrease
                                    0f, 1f, 0f, 0f, 0f,      // Green
                                    0f, 0f, 1.3f, 0f, 20f,   // Blue boost
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.WarmTint -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    1.3f, 0f, 0f, 0f, 30f,   // Red boost
                                    0f, 1f, 0f, 0f, 0f,      // Green
                                    0f, 0f, 0.8f, 0f, -20f,  // Blue reduce
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.Vintage -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    0.9f, 0.3f, 0.1f, 0f, 5f,
                                    0.2f, 0.8f, 0.2f, 0f, 5f,
                                    0.1f, 0.2f, 0.7f, 0f, -10f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.Film -> ColorMatrixColorFilter(ColorMatrix().apply {
                            // High red + green, faded blue for a film-like tone
                            set(
                                floatArrayOf(
                                    1.2f, 0.1f, 0.1f, 0f, 15f,
                                    0.1f, 1.2f, 0.1f, 0f, 10f,
                                    0.1f, 0.1f, 0.9f, 0f, -10f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.TealOrange -> ColorMatrixColorFilter(ColorMatrix().apply {
                            // Teal shadows, orange highlights – a Hollywood-style grade
                            set(
                                floatArrayOf(
                                    1.2f, 0f, 0f, 0f, 20f,
                                    0f, 1.0f, 0f, 0f, 0f,
                                    0f, 0f, 0.8f, 0f, -10f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.HighContrast -> ColorMatrixColorFilter(ColorMatrix().apply {
                            set(
                                floatArrayOf(
                                    1.5f, 0f, 0f, 0f, -50f,
                                    0f, 1.5f, 0f, 0f, -50f,
                                    0f, 0f, 1.5f, 0f, -50f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        })

                        ImageFilter.BlackWhite -> ColorMatrixColorFilter(ColorMatrix().apply {
                            setSaturation(0f)
                            val contrast = ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        1.4f, 0f, 0f, 0f, -50f,
                                        0f, 1.4f, 0f, 0f, -50f,
                                        0f, 0f, 1.4f, 0f, -50f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            }
                            postConcat(contrast)
                        })

                        else -> null
                    }
                }

                when (element.type) {
                    ElementType.TEXT -> {
                        // Adjust text size based on export scale
                        elementPaint.textSize = element.paintTextSize * scaleFactorX
                        // Text is drawn centered around its (x,y)
                        drawText(element.text, 0f, 0f, elementPaint)
                    }

                    ElementType.IMAGE -> {
                        element.bitmap?.let {
                            drawBitmap(it, -it.width / 2f, -it.height / 2f, elementPaint)
                        }
                    }

                    else -> { /* Handle other element types if necessary */
                    }
                }
            }
        }
        return outputBitmap
    }

    private fun checkAlignment(element: CanvasElement) {
        val centerThreshold = 5f // pixels within which we consider centered

        // Check horizontal alignment
        showVerticalGuide = abs(element.x - canvasWidth / 2f) < centerThreshold

        // Check vertical alignment
        showHorizontalGuide = abs(element.y - canvasHeight / 2f) < centerThreshold
    }

    /**
     * Checks if the element's rotation is close to 0, 90, 180, or 270 degrees
     * and sets the rotation alignment guide flags accordingly.
     */
    private fun checkRotationAlignment(element: CanvasElement) {
        val rotationThreshold = 5f // degrees within which we consider aligned

        // Normalize rotation to be between 0 and 360
        val normalizedRotation = (element.rotation % 360 + 360) % 360

        showRotationVerticalGuide = false
        showRotationHorizontalGuide = false

        // Check for 0 or 180 degrees (vertical alignment)
        if (abs(normalizedRotation) < rotationThreshold || abs(normalizedRotation - 180) < rotationThreshold) {
            showRotationVerticalGuide = true
        }

        // Check for 90 or 270 degrees (horizontal alignment)
        if (abs(normalizedRotation - 90) < rotationThreshold || abs(normalizedRotation - 270) < rotationThreshold) {
            showRotationHorizontalGuide = true
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        val widthRatio = parentWidth.toFloat() / canvasWidth
        val heightRatio = parentHeight.toFloat() / canvasHeight
        scale = minOf(widthRatio, heightRatio)

        setMeasuredDimension(parentWidth, parentHeight)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaledWidth = canvasWidth * scale
        val scaledHeight = canvasHeight * scale
        offsetX = (width - scaledWidth) / 2f
        offsetY = (height - scaledHeight) / 2f

        canvas.withTranslation(offsetX, offsetY) {
            scale(scale, scale)

            if (backgroundImage != null) {
                drawBitmap(backgroundImage!!, 0f, 0f, null)
            } else if (backgroundGradient != null) {
                val gradientPaint = Paint().apply { shader = backgroundGradient }
                drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), gradientPaint)
            } else {
                drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), backgroundPaint)
            }

            // Draw alignment guides before elements
            if (showVerticalGuide) {
                drawLine(
                    canvasWidth / 2f, 0f,
                    canvasWidth / 2f, canvasHeight.toFloat(),
                    alignmentPaint
                )
            }

            if (showHorizontalGuide) {
                drawLine(
                    0f, canvasHeight / 2f,
                    canvasWidth.toFloat(), canvasHeight / 2f,
                    alignmentPaint
                )
            }

            // Draw rotation alignment guides
            if (showRotationVerticalGuide) {
                // Draw a vertical line through the center of the canvas
                drawLine(
                    canvasWidth / 2f, 0f,
                    canvasWidth / 2f, canvasHeight.toFloat(),
                    alignmentPaint
                )
            }

            if (showRotationHorizontalGuide) {
                // Draw a horizontal line through the center of the canvas
                drawLine(
                    0f, canvasHeight / 2f,
                    canvasWidth.toFloat(), canvasHeight / 2f,
                    alignmentPaint
                )
            }

            // Draw all elements
            canvasElements.sortedBy { it.zIndex }.forEach { element ->
                canvas.withTranslation(element.x, element.y) {
                    canvas.rotate(element.rotation)
                    canvas.scale(element.scale, element.scale)

                    if (element.type == ElementType.TEXT) {
                        drawTextElement(canvas, element)
                    } else {
                        element.paint.colorFilter = when (element.imageFilter) {
                            ImageFilter.Grayscale -> ColorMatrixColorFilter(ColorMatrix().apply {
                                setSaturation(0f)
                            })

                            ImageFilter.Sepia -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        0.393f, 0.769f, 0.189f, 0f, 0f,
                                        0.349f, 0.686f, 0.168f, 0f, 0f,
                                        0.272f, 0.534f, 0.131f, 0f, 0f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.Invert -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.CoolTint -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        1.1f, 0f, 0f, 0f, -20f,  // Red decrease
                                        0f, 1f, 0f, 0f, 0f,      // Green
                                        0f, 0f, 1.3f, 0f, 20f,   // Blue boost
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.WarmTint -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        1.3f, 0f, 0f, 0f, 30f,   // Red boost
                                        0f, 1f, 0f, 0f, 0f,      // Green
                                        0f, 0f, 0.8f, 0f, -20f,  // Blue reduce
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.Vintage -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        0.9f, 0.3f, 0.1f, 0f, 5f,
                                        0.2f, 0.8f, 0.2f, 0f, 5f,
                                        0.1f, 0.2f, 0.7f, 0f, -10f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.Film -> ColorMatrixColorFilter(ColorMatrix().apply {
                                // High red + green, faded blue for a film-like tone
                                set(
                                    floatArrayOf(
                                        1.2f, 0.1f, 0.1f, 0f, 15f,
                                        0.1f, 1.2f, 0.1f, 0f, 10f,
                                        0.1f, 0.1f, 0.9f, 0f, -10f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.TealOrange -> ColorMatrixColorFilter(ColorMatrix().apply {
                                // Teal shadows, orange highlights – a Hollywood-style grade
                                set(
                                    floatArrayOf(
                                        1.2f, 0f, 0f, 0f, 20f,
                                        0f, 1.0f, 0f, 0f, 0f,
                                        0f, 0f, 0.8f, 0f, -10f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.HighContrast -> ColorMatrixColorFilter(ColorMatrix().apply {
                                set(
                                    floatArrayOf(
                                        1.5f, 0f, 0f, 0f, -50f,
                                        0f, 1.5f, 0f, 0f, -50f,
                                        0f, 0f, 1.5f, 0f, -50f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            })

                            ImageFilter.BlackWhite -> ColorMatrixColorFilter(ColorMatrix().apply {
                                setSaturation(0f)
                                val contrast = ColorMatrix().apply {
                                    set(
                                        floatArrayOf(
                                            1.4f, 0f, 0f, 0f, -50f,
                                            0f, 1.4f, 0f, 0f, -50f,
                                            0f, 0f, 1.4f, 0f, -50f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                }
                                postConcat(contrast)
                            })

                            else -> null
                        }

                        element.bitmap?.let {
                            canvas.drawBitmap(it, -it.width / 2f, -it.height / 2f, element.paint)
                        }
                    }
                }
            }

            // --- Draw combined bounding box and icons based on selection state ---
            if (selectedElements.isNotEmpty()) {
                val combinedBounds = getCombinedSelectedBounds()

                val desiredScreenPadding = 10f
                val localSpacePadding =
                    desiredScreenPadding / scale // Scale padding based on canvas scale

                val desiredScreenStrokeWidth = 2f
                val localSpaceStrokeWidth = desiredScreenStrokeWidth / scale // Scale stroke width

                val dashLengthOnScreen = 10f
                val gapLengthOnScreen = 10f
                val localDashLength = dashLengthOnScreen / scale
                val localGapLength = gapLengthOnScreen / scale

                val boxPaint = Paint().apply {
                    color = Color.GRAY
                    style = Paint.Style.STROKE
                    pathEffect =
                        DashPathEffect(floatArrayOf(localDashLength, localGapLength), 0f)
                    strokeWidth = localSpaceStrokeWidth
                }

                // Draw the single combined bounding box
                canvas.drawRect(
                    combinedBounds.left - localSpacePadding,
                    combinedBounds.top - localSpacePadding,
                    combinedBounds.right + localSpacePadding,
                    combinedBounds.bottom + localSpacePadding,
                    boxPaint
                )

                // Draw icons if elements are selected and not locked
                if (selectedElements.any { !it.isLocked }) { // Draw icons if at least one selected element is not locked
                    val localIconDrawWidth = desiredIconScreenSizePx / scale
                    val localIconDrawHeight = desiredIconScreenSizePx / scale

                    // --- Icon positions for multi-selection or single element selection ---
                    val iconMap = mutableMapOf<String, Pair<Float, Float>>()

                    if (selectedElements.size > 1) { // Multi-selection icons
                        // Remove icon (top-right)
                        iconMap["delete"] = Pair(
                            combinedBounds.right + localSpacePadding,
                            combinedBounds.top - localSpacePadding
                        )
                        // Resize icon (bottom-left)
                        iconMap["resize"] = Pair(
                            combinedBounds.left - localSpacePadding,
                            combinedBounds.bottom + localSpacePadding
                        )
                        // Rotate icon (bottom-right)
                        iconMap["rotate"] = Pair(
                            combinedBounds.right + localSpacePadding,
                            combinedBounds.bottom + localSpacePadding
                        )
                        // Edit icon should not be there for multi-selection
                    } else if (selectedElements.size == 1) { // Single element selection icons
                        val element = selectedElements.first()
                        val elementIconPositions = element.getIconPositions()

                        // Transform element's local icon positions to canvas coordinates
                        val matrix = Matrix()
                        matrix.postRotate(element.rotation)
                        matrix.postScale(element.scale, element.scale)
                        matrix.postTranslate(element.x, element.y)

                        elementIconPositions.forEach { (iconName, position) ->
                            val iconCenterInCanvasCords = floatArrayOf(position.x, position.y)
                            matrix.mapPoints(iconCenterInCanvasCords)
                            iconMap[iconName] =
                                Pair(iconCenterInCanvasCords[0], iconCenterInCanvasCords[1])
                        }
                    }

                    iconMap.forEach { (iconName, position) ->
                        val iconBitmap = when (iconName) {
                            "delete" -> removeIcon
                            "rotate" -> rotateIcon
                            "resize" -> resizeIcon
                            "edit" -> editIcon
                            else -> null
                        }

                        iconBitmap?.let { bmp ->
                            val dstRect = RectF(
                                position.first - localIconDrawWidth / 2f,
                                position.second - localIconDrawHeight / 2f,
                                position.first + localIconDrawWidth / 2f,
                                position.second + localIconDrawHeight / 2f
                            )
                            canvas.drawBitmap(bmp, null, dstRect, null)
                        }
                    }
                }
            }
        }
    }

    private fun drawTextElement(canvas: Canvas, element: CanvasElement) {
        val lines = element.text.split("\n")
        val fm = element.paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * element.lineSpacing
        val totalHeight = lineHeight * lines.size

        if (element.hasLabel) {
            val maxLineWidth = lines.maxOf { element.paint.measureText(it) }
            val labelPadding = 16f
            val left = -maxLineWidth / 2f - labelPadding
            val top = -totalHeight / 2f - labelPadding
            val right = maxLineWidth / 2f + labelPadding
            val bottom = totalHeight / 2f + labelPadding

            val labelRect = RectF(left, top, right, bottom)
            val labelPaint = Paint().apply {
                color = element.labelColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            when (element.labelShape) {
                LabelShape.RECTANGLE_FILL -> {
                    labelPaint.style = Paint.Style.FILL
                    canvas.drawRect(labelRect, labelPaint)
                }

                LabelShape.RECTANGLE_STROKE -> {
                    labelPaint.style = Paint.Style.STROKE
                    labelPaint.strokeWidth = 4f // You can adjust the stroke width as needed
                    canvas.drawRect(labelRect, labelPaint)
                }

                LabelShape.OVAL_FILL -> {
                    labelPaint.style = Paint.Style.FILL
                    canvas.drawOval(labelRect, labelPaint)
                }

                LabelShape.OVAL_STROKE -> {
                    labelPaint.style = Paint.Style.STROKE
                    labelPaint.strokeWidth = 4f // Adjust stroke width as needed
                    canvas.drawOval(labelRect, labelPaint)
                }

                LabelShape.CIRCLE_FILL -> {
                    labelPaint.style = Paint.Style.FILL
                    val radius = Math.min(labelRect.width(), labelRect.height()) / 2f
                    val centerX = labelRect.centerX()
                    val centerY = labelRect.centerY()
                    canvas.drawCircle(centerX, centerY, radius, labelPaint)
                }

                LabelShape.CIRCLE_STROKE -> {
                    labelPaint.style = Paint.Style.STROKE
                    labelPaint.strokeWidth = 4f // Adjust stroke width as needed
                    val radius = Math.min(labelRect.width(), labelRect.height()) / 2f
                    val centerX = labelRect.centerX()
                    val centerY = labelRect.centerY()
                    canvas.drawCircle(centerX, centerY, radius, labelPaint)
                }

                LabelShape.ROUNDED_RECTANGLE_FILL -> {
                    labelPaint.style = Paint.Style.FILL
                    canvas.drawRoundRect(
                        labelRect,
                        20f,
                        20f,
                        labelPaint
                    ) // Adjust corner radius as needed
                }

                LabelShape.ROUNDED_RECTANGLE_STROKE -> {
                    labelPaint.style = Paint.Style.STROKE
                    labelPaint.strokeWidth = 4f // Adjust stroke width as needed
                    canvas.drawRoundRect(
                        labelRect,
                        20f,
                        20f,
                        labelPaint
                    ) // Adjust corner radius as needed
                }

                else -> {
                    // Default to drawing a rectangle if no shape matches
                    labelPaint.style = Paint.Style.FILL
                    canvas.drawRect(labelRect, labelPaint)
                }
            }
        }

        // Font correction for baseline alignment
        val baselineShift = (fm.ascent + fm.descent) / 2f
        var yOffset = -((lines.size - 1) * lineHeight / 2f) - baselineShift

        lines.forEachIndexed { i, line ->

            val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = element.paintColor
                textSize = element.paintTextSize
                alpha = element.paintAlpha
                letterSpacing = element.letterSpacing
                isAntiAlias = true
                style = Paint.Style.FILL

                // Underline
                isUnderlineText = TextDecoration.UNDERLINE in element.textDecoration

                // Bold / Italic
                val baseTypeface = element.paint.typeface ?: Typeface.DEFAULT
                val isBold = TextDecoration.BOLD in element.textDecoration
                val isItalic = TextDecoration.ITALIC in element.textDecoration

                val style = when {
                    isBold && isItalic -> Typeface.BOLD_ITALIC
                    isBold -> Typeface.BOLD
                    isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                typeface = Typeface.create(baseTypeface, style)
            }

            // Handle list styles
            val textToDraw = when (element.listStyle) {
                ListStyle.BULLETED -> "• $line"
                ListStyle.NUMBERED -> "${i + 1}. $line"
                else -> line
            }

            // Letter casing
            val displayText = when (element.letterCasing) {
                LetterCasing.ALL_CAPS -> textToDraw.uppercase()
                LetterCasing.LOWER_CASE -> textToDraw.lowercase()
                LetterCasing.TITLE_CASE -> textToDraw.split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

                else -> textToDraw
            }

            // Alignment
            val alignment = when (element.alignment) {
                TextAlignment.LEFT -> Paint.Align.LEFT
                TextAlignment.CENTER -> Paint.Align.CENTER
                TextAlignment.RIGHT -> Paint.Align.RIGHT
                TextAlignment.JUSTIFY -> Paint.Align.LEFT // needed for justify, but handled separately
            }

            val indentOffset = if (i == 0) element.currentIndent else 0f

            fillPaint.textAlign = alignment
            val xPosition = when (alignment) {
                Paint.Align.LEFT -> -element.getLocalContentWidth() / 2f + indentOffset
                Paint.Align.CENTER -> 0f
                Paint.Align.RIGHT -> element.getLocalContentWidth() / 2f + indentOffset
                else -> 0f
            }

            element.fillGradientColors?.let { colors ->
                // measure the widest line so your gradient spans the text
                val maxLineWidth = lines.maxOf { fillPaint.measureText(it) }
                fillPaint.shader = LinearGradient(
                    -maxLineWidth / 2f, 0f,
                    maxLineWidth / 2f, 0f,
                    colors,
                    element.fillGradientPositions,
                    Shader.TileMode.CLAMP
                )
            } ?: run {
                fillPaint.shader = null
                fillPaint.color = element.paintColor
            }

            if (element.hasBlur) {
                val blurMaskFilter = BlurMaskFilter(element.blurValue, BlurMaskFilter.Blur.NORMAL)
                fillPaint.maskFilter = blurMaskFilter
            }

            // Apply opacity (alpha)
            fillPaint.alpha = element.paintAlpha

            // Apply layer blending (based on imageFilter)
            when (element.blendType) {
                BlendType.MULTIPLY -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                BlendType.SRC_OVER -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                BlendType.SCREEN -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                BlendType.ADD -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                BlendType.LIGHTEN -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
                BlendType.DARKEN -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                BlendType.SRC -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                BlendType.DST -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST)
                BlendType.DST_OVER -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                BlendType.SRC_IN -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                BlendType.DST_IN -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                BlendType.SRC_OUT -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
                BlendType.DST_OUT -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                BlendType.SRC_ATOP -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                BlendType.DST_ATOP -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
                BlendType.XOR -> fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
            }

            if (element.hasShadow) {
                val shadowColorWithOpacity =
                    (element.shadowColor and 0x00FFFFFF) or (element.shadowOpacity shl 24)
                val shadowPaint = TextPaint(fillPaint).apply {
                    setShadowLayer(
                        element.shadowRadius,
                        element.shadowDx,
                        element.shadowDy,
                        shadowColorWithOpacity
                    )
                    shader = null
                }
                canvas.drawText(displayText, xPosition, yOffset, shadowPaint)
            }

            // Handle justified text separately
            if (element.alignment == TextAlignment.JUSTIFY) {
                element.paint = fillPaint
                justifyText(canvas, displayText, yOffset, element)
            } else {
                // Draw filled text
                canvas.drawText(displayText, xPosition, yOffset, fillPaint)

                // Draw border (stroke) if needed
                if (element.hasStroke && element.strokeWidth > 0f) {
                    val strokePaint = TextPaint(fillPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = element.strokeWidth
                        element.strokeGradientColors?.let { sColors ->
                            // use same width to span stroke gradient
                            val w = element.strokeWidth
                            shader = LinearGradient(
                                -w / 2f, 0f,
                                w / 2f, 0f,
                                sColors,
                                element.strokeGradientPositions ?: floatArrayOf(0f, 1f),
                                Shader.TileMode.CLAMP
                            )
                        } ?: run {
                            shader = null
                            color = element.strokeColor
                        }
                        textAlign = alignment
                    }
                    canvas.drawText(displayText, xPosition, yOffset, strokePaint)
                }
            }

            yOffset += lineHeight
        }
    }

    private fun justifyText(canvas: Canvas, text: String, yOffset: Float, element: CanvasElement) {
        if (text.length <= 1) {
            val x = -element.getLocalContentWidth() / 2f
            canvas.drawText(text, x, yOffset, element.paint)
            return
        }

        val basePaint = TextPaint(element.paint).apply {
            isAntiAlias = true
            letterSpacing = element.letterSpacing
            textAlign = Paint.Align.LEFT
        }

        // Create FILL and STROKE paints, both retaining shadow and font features
        val fillPaint = TextPaint(basePaint).apply {
            style = Paint.Style.FILL
            color = element.paintColor
        }

        val strokePaint = if (element.hasStroke && element.strokeWidth > 0f) {
            TextPaint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = element.strokeWidth
                color = element.strokeColor
            }
        } else null

        val charWidths = text.map { fillPaint.measureText(it.toString()) }
        val textWidth = charWidths.sum()
        val totalAvailable = element.getLocalContentWidth()
        val extraSpace = (totalAvailable - textWidth) / (text.length - 1)

        var xOffset = -totalAvailable / 2f

        text.forEachIndexed { index, char ->
            val charStr = char.toString()
            // Draw stroke first
            strokePaint?.let { canvas.drawText(charStr, xOffset, yOffset, it) }
            // Then draw fill
            canvas.drawText(charStr, xOffset, yOffset, fillPaint)
            // Move to next char position
            xOffset += charWidths[index] + extraSpace
        }
    }

    // Helper functions for pinch distance and angle
    private fun getPinchDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return hypot(x.toDouble(), y.toDouble()).toFloat()
    }

    private fun getPinchAngle(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = (e.x - offsetX) / scale
            val y = (e.y - offsetY) / scale

            val touchedElement =
                canvasElements.sortedByDescending { it.zIndex }.firstOrNull { element ->
                    val matrix = Matrix()
                    matrix.postTranslate(-element.x, -element.y)
                    matrix.postRotate(-element.rotation)
                    matrix.postScale(1f / element.scale, 1f / element.scale)

                    val touchPoint = floatArrayOf(x, y)
                    matrix.mapPoints(touchPoint)

                    val bounds = RectF(
                        -element.getLocalContentWidth() / 2f,
                        -element.getLocalContentHeight() / 2f,
                        element.getLocalContentWidth() / 2f,
                        element.getLocalContentHeight() / 2f
                    )
                    bounds.contains(touchPoint[0], touchPoint[1])
                }

            if (touchedElement != null) {
                // Deselect all existing elements before selecting the new one
                canvasElements.forEach { it.isSelected = false }
                selectedElements.clear() // Clear internal selected list as well
                touchedElement.isSelected = true // Select the new element
                selectedElements.add(touchedElement) // Add to internal selected list
                onElementSelected?.invoke(selectedElements) // Notify ViewModel of the new single selection
                onEditTextRequested?.invoke(touchedElement)
                invalidate()
                return true
            }
            return false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val x = (event.x - offsetX) / scale
        val y = (event.y - offsetY) / scale

        when (event.actionMasked) {

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    currentMode = Mode.MULTI_TOUCH
                    initialPinchDistance = getPinchDistance(event)
                    initialPinchAngle = getPinchAngle(event)
                    initialScale = selectedElements.firstOrNull()?.scale
                        ?: 1f // Get initial scale of the first selected element
                    initialRotation =
                        selectedElements.firstOrNull()?.rotation ?: 0f // Get initial rotation
                }
            }

            MotionEvent.ACTION_DOWN -> {
                iconTouched = null // Reset touched icon
                lastTouchedElement = null // Reset last touched element
                showVerticalGuide = false
                showHorizontalGuide = false
                showRotationVerticalGuide = false // Reset rotation guides
                showRotationHorizontalGuide = false // Reset rotation guides


                // 1. Check for icon touch (regardless of single or multi-selection, based on combined bounds)
                if (selectedElements.isNotEmpty() && selectedElements.any { !it.isLocked }) {
                    val combinedBounds = getCombinedSelectedBounds()
                    val localSpacePadding = 10f / scale // Use the same padding as drawing
                    val adjustedIconHitSize = desiredIconScreenSizePx / scale * 1.5f

                    // Define "global" icon positions based on combined bounds
                    val globalIconRegions = mutableMapOf<String, RectF>()

                    if (selectedElements.size > 1) { // Multi-selection icon hit areas
                        // Delete icon (top-right of combined bounds)
                        globalIconRegions["delete"] = RectF(
                            combinedBounds.right + localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.top - localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.right + localSpacePadding + adjustedIconHitSize / 2f,
                            combinedBounds.top - localSpacePadding + adjustedIconHitSize / 2f
                        )
                        // Resize icon (bottom-left of combined bounds)
                        globalIconRegions["resize"] = RectF(
                            combinedBounds.left - localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.left - localSpacePadding + adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding + adjustedIconHitSize / 2f
                        )
                        // Rotate icon (bottom-right of combined bounds)
                        globalIconRegions["rotate"] = RectF(
                            combinedBounds.right + localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.right + localSpacePadding + adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding + adjustedIconHitSize / 2f
                        )
                        // No edit icon for multi-selection
                    } else { // Single element selection icon hit areas
                        val element = selectedElements.first()
                        val elementIconPositions =
                            element.getIconPositions() // These are element-local positions

                        // Convert element-local icon positions to canvas coordinates for hit testing
                        val elementMatrix = Matrix()
                        elementMatrix.postRotate(element.rotation)
                        elementMatrix.postScale(element.scale, element.scale)
                        elementMatrix.postTranslate(element.x, element.y)

                        elementIconPositions.forEach { (iconName, position) ->
                            val iconCenterInCanvasCords = floatArrayOf(position.x, position.y)
                            elementMatrix.mapPoints(iconCenterInCanvasCords)

                            globalIconRegions[iconName] = RectF(
                                iconCenterInCanvasCords[0] - adjustedIconHitSize / 2f,
                                iconCenterInCanvasCords[1] - adjustedIconHitSize / 2f,
                                iconCenterInCanvasCords[0] + adjustedIconHitSize / 2f,
                                iconCenterInCanvasCords[1] + adjustedIconHitSize / 2f
                            )
                        }
                    }

                    val touchedIconEntry = globalIconRegions.entries.firstOrNull { (_, rect) ->
                        rect.contains(x, y)
                    }

                    if (touchedIconEntry != null) {
                        iconTouched = touchedIconEntry.key
                        // For icon interaction, we often operate on the entire selection or a specific element related to the icon
                        when (iconTouched) {
                            "delete" -> {
                                removeSelectedElement() // Handles removing all selected
                                return true // Consume the event immediately
                            }

                            "rotate" -> {
                                currentMode = Mode.ROTATE
                                touchStartX =
                                    x // Store the initial touch point for angle calculation
                                touchStartY = y

                                initialElementRotations.clear()
                                initialElementPositionsRelativeToGroupPivot.clear() // Clear previous initial positions
                                val combinedBoundsAtStart =
                                    getCombinedSelectedBounds() // Get bounds at start of interaction
                                initialGroupPivotX = combinedBoundsAtStart.centerX()
                                initialGroupPivotY = combinedBoundsAtStart.centerY()

                                selectedElements.forEach { element ->
                                    initialElementRotations[element.id] = element.rotation
                                    // Store initial position relative to the group's center
                                    initialElementPositionsRelativeToGroupPivot[element.id] =
                                        Pair(
                                            element.x - initialGroupPivotX,
                                            element.y - initialGroupPivotY
                                        )
                                }
                                initialAngle = atan2(
                                    touchStartY - initialGroupPivotY,
                                    touchStartX - initialGroupPivotX
                                ) // Initial angle for rotation calculation
                                selectedElements.firstOrNull()?.let { element ->
                                    onStartBatchUpdate?.invoke(element.id, "rotate")
                                }
                                return true
                            }

                            "resize" -> {
                                currentMode = Mode.RESIZE
                                touchStartX = x
                                touchStartY = y
                                selectedElements.firstOrNull()?.let { element ->
                                    onStartBatchUpdate?.invoke(element.id, "resize")
                                }
                                return true
                            }

                            "edit" -> {
                                if (selectedElements.size == 1) {
                                    onEditTextRequested?.invoke(selectedElements.first())
                                }
                                return true
                            }
                        }
                    }
                }

                // 2. If no icon was touched, check for element touch (single or multi-selection)
                val touchedElement =
                    canvasElements.sortedByDescending { it.zIndex }.firstOrNull { element ->
                        val matrix = Matrix()
                        matrix.postTranslate(-element.x, -element.y)
                        matrix.postRotate(-element.rotation)
                        matrix.postScale(1f / element.scale, 1f / element.scale)

                        val touchPoint = floatArrayOf(x, y)
                        matrix.mapPoints(touchPoint)

                        val bounds = RectF(
                            -element.getLocalContentWidth() / 2f,
                            -element.getLocalContentHeight() / 2f,
                            element.getLocalContentWidth() / 2f,
                            element.getLocalContentHeight() / 2f
                        )
                        bounds.contains(touchPoint[0], touchPoint[1])
                    }

                if (touchedElement != null) {
                    if (touchedElement.isSelected) {
                        // If an already selected element is tapped, assume user wants to drag the group
                        // Do NOT deselect others.
                        lastTouchedElement = touchedElement // Set the one that initiated the drag
                        currentMode = Mode.DRAG
                        touchStartX = x
                        touchStartY = y
                    } else {
                        // If an unselected element is tapped, deselect all others and select only this one
                        canvasElements.forEach { it.isSelected = false } // Deselect all
                        selectedElements.clear() // Clear internal selected list
                        touchedElement.isSelected = true // Select the new element
                        selectedElements.add(touchedElement) // Add to internal selected list
                        lastTouchedElement = touchedElement // Set for drag
                        currentMode = Mode.DRAG
                        touchStartX = x
                        touchStartY = y
                    }
                    onStartBatchUpdate?.invoke(touchedElement.id, "drag")
                    onElementSelected?.invoke(selectedElements)
                    invalidate()
                    return true
                } else {
                    // 3. Tapped on empty canvas, deselect all elements
                    if (selectedElements.isNotEmpty()) {
                        canvasElements.forEach { it.isSelected = false }
                        selectedElements.clear()
                        onElementSelected?.invoke(selectedElements) // Notify ViewModel of empty selection
                        invalidate()
                    }
                    currentMode = Mode.NONE // Reset mode
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Determine which elements to modify based on current mode and touch context
                val elementsToModify = selectedElements.filter { !it.isLocked }

                if (elementsToModify.isEmpty()) return true // No elements to modify

                when (currentMode) {
                    Mode.DRAG -> {
                        val dx = x - touchStartX
                        val dy = y - touchStartY

                        val combinedBounds = getCombinedSelectedBounds()
                        val newLeft = combinedBounds.left + dx
                        val newTop = combinedBounds.top + dy

                        // Clamp the new combined bounds to the canvas
                        val maxLeft = max(0f, canvasWidth - combinedBounds.width())
                        val clampedLeft = newLeft.coerceIn(0f, maxLeft)
                        val maxTop = max(0f, canvasHeight - combinedBounds.height())
                        val clampedTop = newTop.coerceIn(0f, maxTop)

                        val actualDx = clampedLeft - combinedBounds.left
                        val actualDy = clampedTop - combinedBounds.top

                        elementsToModify.forEach { element ->
                            element.x += actualDx
                            element.y += actualDy
                            val halfW = element.getLocalContentWidth() / 2f
                            val halfH = element.getLocalContentHeight() / 2f
                            element.x = element.x.coerceIn(halfW, canvasWidth - halfW)
                            element.y = element.y.coerceIn(halfH, canvasHeight - halfH)
                            onElementChanged?.invoke(element)
                        }

                        // Check alignment for the first selected element (if only one is selected for single drag)
                        if (selectedElements.size == 1) {
                            checkAlignment(selectedElements.first())
                        } else {
                            showVerticalGuide = false
                            showHorizontalGuide = false
                        }

                        touchStartX = x // Update touch start for continuous drag
                        touchStartY = y
                        invalidate()
                    }

                    Mode.MULTI_TOUCH -> {
                        if (event.pointerCount >= 2) {
                            val newPinchDistance = getPinchDistance(event)
                            val newPinchAngle = getPinchAngle(event)

                            // Scale
                            if (initialPinchDistance > 0) {
                                val scaleFactor = newPinchDistance / initialPinchDistance
                                selectedElements.filter { !it.isLocked }.forEach { element ->
                                    val newScale = (initialScale * scaleFactor).coerceIn(
                                        0.1f,
                                        5f
                                    ) // Apply to initial scale
                                    element.scale = newScale
                                    onElementChanged?.invoke(element)
                                }
                            }

                            // Rotate
                            val rotationDelta = newPinchAngle - initialPinchAngle
                            selectedElements.filter { !it.isLocked }.forEach { element ->
                                element.rotation = (initialRotation + rotationDelta) % 360
                                onElementChanged?.invoke(element)
                            }
                            invalidate()
                        }
                    }

                    Mode.ROTATE -> {
                        if (selectedElements.isEmpty()) return true

                        val currentAngle = atan2(
                            y - initialGroupPivotY,
                            x - initialGroupPivotX
                        ) // Calculate angle relative to initial group pivot
                        val deltaAngle =
                            Math.toDegrees((currentAngle - initialAngle).toDouble()).toFloat()

                        elementsToModify.forEach { element ->
                            val initialRotation =
                                initialElementRotations[element.id] ?: element.rotation
                            element.rotation =
                                (initialRotation + deltaAngle) % 360 // Update element's own rotation

                            // Rotate element's initial position relative to the group pivot
                            val initialRelativeX =
                                initialElementPositionsRelativeToGroupPivot[element.id]?.first ?: 0f
                            val initialRelativeY =
                                initialElementPositionsRelativeToGroupPivot[element.id]?.second
                                    ?: 0f

                            val rotatedRelativeX =
                                (initialRelativeX * kotlin.math.cos(Math.toRadians(deltaAngle.toDouble()))) - (initialRelativeY * kotlin.math.sin(
                                    Math.toRadians(deltaAngle.toDouble())
                                ))
                            val rotatedRelativeY =
                                (initialRelativeX * kotlin.math.sin(Math.toRadians(deltaAngle.toDouble()))) + (initialRelativeY * kotlin.math.cos(
                                    Math.toRadians(deltaAngle.toDouble())
                                ))

                            // Update element's position based on the rotated relative position from the *initial* group pivot
                            element.x = initialGroupPivotX + rotatedRelativeX.toFloat()
                            element.y = initialGroupPivotY + rotatedRelativeY.toFloat()

                            onElementChanged?.invoke(element)
                        }

                        // After rotating, re-calculate the combined bounds to check for clamping
                        val newCombinedBounds = getCombinedSelectedBounds()

                        // Clamp the rotated group back into the canvas if it went out
                        var translationX = 0f
                        var translationY = 0f

                        if (newCombinedBounds.left < 0) {
                            translationX = -newCombinedBounds.left
                        } else if (newCombinedBounds.right > canvasWidth) {
                            translationX = canvasWidth - newCombinedBounds.right
                        }

                        if (newCombinedBounds.top < 0) {
                            translationY = -newCombinedBounds.top
                        } else if (newCombinedBounds.bottom > canvasHeight) {
                            translationY = canvasHeight - newCombinedBounds.bottom
                        }

                        if (translationX != 0f || translationY != 0f) {
                            elementsToModify.forEach { element ->
                                element.x += translationX
                                element.y += translationY
                                onElementChanged?.invoke(element)
                            }
                            // Also adjust the initialGroupPivotX and Y to reflect the new clamped position
                            initialGroupPivotX += translationX
                            initialGroupPivotY += translationY
                        }

                        // Check rotation alignment for the first selected element
                        if (selectedElements.size == 1) {
                            checkRotationAlignment(selectedElements.first())
                        } else {
                            showRotationVerticalGuide = false
                            showRotationHorizontalGuide = false
                        }

                        invalidate()
                    }


                    Mode.RESIZE -> {
                        if (selectedElements.isEmpty()) return true

                        val combinedBounds = getCombinedSelectedBounds()
                        val pivotX = combinedBounds.centerX()
                        val pivotY = combinedBounds.centerY()

                        // Calculate distances from the pivot to the touch start and current touch points
                        val startDist = hypot(touchStartX - pivotX, touchStartY - pivotY)
                        val currentDist = hypot(x - pivotX, y - pivotY)

                        if (startDist > 0) {
                            val scaleChange = currentDist / startDist
                            elementsToModify.forEach { element ->
                                // Calculate new scale for the individual element
                                val newScale = (element.scale * scaleChange).coerceIn(0.1f, 5f)

                                // To resize around the group's center, we need to adjust element's position as well
                                // Calculate vector from group pivot to element's center
                                val vecX = element.x - pivotX
                                val vecY = element.y - pivotY

                                // Scale this vector
                                val scaledVecX = vecX * scaleChange
                                val scaledVecY = vecY * scaleChange

                                // New position of element relative to group pivot
                                element.x = pivotX + scaledVecX
                                element.y = pivotY + scaledVecY
                                element.scale = newScale

                                onElementChanged?.invoke(element) // Notify for each changed element
                            }
                        }
                        touchStartX = x // Update touch start for continuous scaling
                        touchStartY = y
                        invalidate()
                    }

                    Mode.NONE -> {
                        // This block handles potential tap-and-hold to drag if not immediately picking up an icon/element
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                showVerticalGuide = false
                showHorizontalGuide = false
                showRotationVerticalGuide = false // Reset rotation guides on ACTION_UP
                showRotationHorizontalGuide = false // Reset rotation guides on ACTION_UP


                if (currentMode == Mode.DRAG || currentMode == Mode.ROTATE || currentMode == Mode.RESIZE) {
                    selectedElements.filter { !it.isLocked }.forEach {
                        onElementChanged?.invoke(it)
                        onEndBatchUpdate?.invoke(it.id)
                    }
                }

                iconTouched = null
                lastTouchedElement = null
                currentMode = Mode.NONE
                initialPinchDistance = 0f
                initialPinchAngle = 0f
                initialScale = 1f
                initialRotation = 0f
                initialElementRotations.clear()
                initialElementPositionsRelativeToGroupPivot.clear() // Clear initial positions on action up
                initialAngle = 0f
                initialGroupPivotX = 0f
                initialGroupPivotY = 0f
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
}