package com.example.urduphotodesigner.common.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.model.ExportOptions
import com.example.urduphotodesigner.common.enums.ElementType
import com.example.urduphotodesigner.common.enums.Mode
import com.example.urduphotodesigner.data.model.FontEntity
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import androidx.core.graphics.withScale

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
        canvasElements.addAll(newElements) // Add copies to avoid direct modification issues
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

    /**
     * Sets the line spacing for the currently selected text element.
     * Note: With multi-selection, this should ideally apply to all selected text elements.
     * For now, it applies to the first selected text element found.
     */
    fun setLineSpacing(multiplier: Float) {
        // Apply to all selected text elements
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.lineSpacingMultiplier = multiplier
            onElementChanged?.invoke(element) // Notify ViewModel of change
        }
        invalidate()
    }

    fun clearCanvas() {
        canvasElements.clear()
        selectedElements.clear()
        backgroundPaint.color = Color.WHITE // Or your default background color
        backgroundGradient = null
        backgroundImage = null
        invalidate()
    }

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

    fun setTextAlignment(alignment: Paint.Align) {
        selectedElements.filter { it.type == ElementType.TEXT }.forEach { element ->
            element.paint.textAlign = alignment
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
        // This typically applies to a single text element, but for multi-selection,
        // you might want to consider how this behaves (e.g., update first selected, or all).
        // For now, let's assume it updates the first selected text element.
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

        // Determine the target output dimensions based on the selected resolution
        if (options.resolution.width == 0 && options.resolution.height == 0) {
            // "Original Size" resolution: use canvas's current size, apply scaleFactor (which is 1f for Original Size)
            outputWidth = (canvasWidth * options.resolution.scaleFactor).toInt()
            outputHeight = (canvasHeight * options.resolution.scaleFactor).toInt()
        } else {
            // Specific resolution: use the resolution's width and height directly
            outputWidth = options.resolution.width
            outputHeight = options.resolution.height
        }

        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate a single uniform scale factor to maintain aspect ratio
        // This scale factor will fit the original canvas content into the new output dimensions
        val scaleFactor = minOf(outputWidth.toFloat() / canvasWidth, outputHeight.toFloat() / canvasHeight)

        // Calculate translation to center the scaled content within the new bitmap
        val scaledContentWidth = canvasWidth * scaleFactor
        val scaledContentHeight = canvasHeight * scaleFactor
        val translateX = (outputWidth - scaledContentWidth) / 2f
        val translateY = (outputHeight - scaledContentHeight) / 2f

        // Apply global translation and scaling to the canvas before drawing
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)

        // Now, draw the background and elements as if drawing on the original canvas size
        // The translation and scaling applied above will handle fitting it into the output bitmap.

        // 1. Draw background
        backgroundImage?.let {
            // Assuming backgroundImage is already scaled/prepared for canvasWidth x canvasHeight.
            canvas.drawBitmap(it, 0f, 0f, null)
        } ?: backgroundGradient?.let {
            canvas.drawRect(
                0f,
                0f,
                canvasWidth.toFloat(), // Draw at original canvas dimensions
                canvasHeight.toFloat(),
                backgroundPaint.apply { shader = it })
        } ?: run {
            canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), backgroundPaint)
        }

        // 2. Draw canvas elements based on their z-index
        canvasElements.sortedBy { it.zIndex }.forEach { element ->
            // Elements' positions (x, y) and scale are relative to the original canvas dimensions.
            // These will be correctly scaled by the global canvas.scale() and translated by canvas.translate().
            canvas.withTranslation(element.x, element.y) {
                rotate(element.rotation)
                scale(element.scale, element.scale)

                when (element.type) {
                    ElementType.TEXT -> {
                        val lines = element.text.split("\n")
                        val fm = element.paint.fontMetrics
                        val lineHeight = (fm.bottom - fm.top) * element.lineSpacingMultiplier
                        val totalHeight = lineHeight * lines.size
                        lines.forEachIndexed { i, line ->
                            val yOffset = -totalHeight / 2 + i * lineHeight - fm.top
                            drawText(line, 0f, yOffset, element.paint)
                        }
                    }

                    ElementType.IMAGE -> {
                        element.bitmap?.let { bmp ->
                            val srcRect = Rect(0, 0, bmp.width, bmp.height)
                            val dstRect = RectF(
                                -element.getLocalContentWidth() / 2f,
                                -element.getLocalContentHeight() / 2f,
                                element.getLocalContentWidth() / 2f,
                                element.getLocalContentHeight() / 2f
                            )
                            drawBitmap(bmp, srcRect, dstRect, element.paint)
                        }
                    }
                }
            }
        }
        return bitmap
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
                        val lines = element.text.split("\n")
                        val fm = element.paint.fontMetrics
                        val lineHeight = (fm.bottom - fm.top) * element.lineSpacingMultiplier
                        val totalHeight = lineHeight * lines.size
                        lines.forEachIndexed { i, line ->
                            val yOffset = -totalHeight / 2 + i * lineHeight - fm.top
                            canvas.drawText(line, 0f, yOffset, element.paint)
                        }
                    } else {
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
                        // Remove icon (top-left)
                        iconMap["delete"] = Pair(
                            combinedBounds.left - localSpacePadding,
                            combinedBounds.top - localSpacePadding
                        )
                        // Rotate icon (bottom-left)
                        iconMap["rotate"] = Pair(
                            combinedBounds.left - localSpacePadding,
                            combinedBounds.bottom + localSpacePadding
                        )
                        // Resize icon (bottom-right)
                        iconMap["resize"] = Pair(
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
                            // Only add "edit" icon if it's a text element
                            if (iconName == "edit" && element.type != ElementType.TEXT) {
                                return@forEach // Skip if it's an edit icon for non-text element
                            }

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

            if (touchedElement != null && touchedElement.type == ElementType.TEXT) {
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
                        // Delete icon (top-left of combined bounds)
                        globalIconRegions["delete"] = RectF(
                            combinedBounds.left - localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.top - localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.left - localSpacePadding + adjustedIconHitSize / 2f,
                            combinedBounds.top - localSpacePadding + adjustedIconHitSize / 2f
                        )
                        // Rotate icon (bottom-left of combined bounds)
                        globalIconRegions["rotate"] = RectF(
                            combinedBounds.left - localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding - adjustedIconHitSize / 2f,
                            combinedBounds.left - localSpacePadding + adjustedIconHitSize / 2f,
                            combinedBounds.bottom + localSpacePadding + adjustedIconHitSize / 2f
                        )
                        // Resize icon (bottom-right of combined bounds)
                        globalIconRegions["resize"] = RectF(
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
                            // Only add "edit" icon hit area if it's a text element
                            if (iconName == "edit" && element.type != ElementType.TEXT) {
                                return@forEach // Skip if it's an edit icon for non-text element
                            }

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
                                if (selectedElements.size == 1 && selectedElements.first().type == ElementType.TEXT) {
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
}
