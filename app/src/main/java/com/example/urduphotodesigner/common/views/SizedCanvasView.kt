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
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasElement
import com.example.urduphotodesigner.common.canvas.ElementType
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
    var onElementSelected: ((CanvasElement) -> Unit)? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val threshold = 20f

    private val desiredIconScreenSizePx = 48f
    private var iconTouched: String? = null

    private var backgroundGradient: LinearGradient? = null
    private var backgroundImage: Bitmap? = null
    private val canvasElements = mutableListOf<CanvasElement>()
//
//    private val selectedElements = mutableSetOf<CanvasElement>()
//    private var selectedElementForAction: CanvasElement? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var currentMode: Mode = Mode.NONE
    private var lastRotation = 0f

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val alignmentPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var showVerticalGuide = false
    private var showHorizontalGuide = false

    private enum class Mode { NONE, DRAG, ROTATE, RESIZE }

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

    fun syncElements(newElements: List<CanvasElement>) {
        canvasElements.clear()
        canvasElements.addAll(newElements.map { it.copy() }) // Add copies to avoid direct modification issues
        invalidate()
    }

    fun setLineSpacing(multiplier: Float) {
        canvasElements.find { it.isSelected }?.lineSpacingMultiplier = multiplier
        invalidate()
    }

    fun clearCanvas() {
        canvasElements.clear()
        backgroundPaint.color = Color.WHITE // Or your default background color
        backgroundGradient = null
        backgroundImage = null
        invalidate() // Redraw the canvas
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
        canvasElements.add(element)
        invalidate()
    }

    fun updateElementZIndex(element: CanvasElement, newZIndex: Int) {
        element.zIndex = newZIndex
        invalidate()
    }

    fun removeSelectedElement() {
        val selected = canvasElements.find { it.isSelected }
        selected?.let {
            canvasElements.remove(it)
            invalidate()
            onElementRemoved?.invoke(it) // Notify ViewModel to remove
        }
        invalidate()
    }

    fun setFont(typeface: Typeface) {
        canvasElements.find { it.isSelected }?.paint?.typeface = typeface
        invalidate()
    }

    fun setTextColor(color: Int) {
        canvasElements.find { it.isSelected }?.paint?.color = color
        invalidate()
    }

    fun setTextSize(size: Float) {
        canvasElements.find { it.isSelected }?.paint?.textSize = size
        invalidate()
    }

    fun setTextAlignment(alignment: Paint.Align) {
        canvasElements.find { it.isSelected }?.paint?.textAlign = alignment
        invalidate()
    }

    fun setOpacity(opacity: Int) {
        canvasElements.find { it.isSelected }?.paint?.alpha = opacity
        invalidate()
    }

    fun updateText(text: String) {
        canvasElements.find { it.isSelected }?.text = text
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
        // Calculate the scale to maintain aspect ratio and fill the canvas
        val scale = max(
            canvasWidth.toFloat() / bitmap.width,
            canvasHeight.toFloat() / bitmap.height
        )

        // Calculate the scaled dimensions
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()

        // Create a scaled bitmap that fills the canvas while maintaining aspect ratio
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // Create a new bitmap with canvas dimensions and draw the scaled bitmap centered
        val resultBitmap = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(resultBitmap)

        // Calculate position to center the scaled bitmap
        val left = (canvasWidth - scaledWidth) / 2f
        val top = (canvasHeight - scaledHeight) / 2f

        canvas.drawBitmap(scaledBitmap, left, top, null)

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
        canvasElements.add(element)
        invalidate()
    }

    private fun checkAlignment(element: CanvasElement) {
        val centerThreshold = 5f // pixels within which we consider centered

        // Check horizontal alignment
        showVerticalGuide = abs(element.x - canvasWidth / 2f) < centerThreshold

        // Check vertical alignment
        showHorizontalGuide = abs(element.y - canvasHeight / 2f) < centerThreshold
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

            canvasElements.sortedBy { it.zIndex }.forEach { element ->
                withTranslation(element.x, element.y) {
                    rotate(element.rotation)
                    scale(element.scale, element.scale)

                    if (element.type == ElementType.TEXT) {
                        val lines = element.text.split("\n")
                        val fm = element.paint.fontMetrics
                        val lineHeight = (fm.bottom - fm.top) * element.lineSpacingMultiplier
                        val totalHeight = lineHeight * lines.size
                        lines.forEachIndexed { i, line ->
                            val yOffset = -totalHeight / 2 + i * lineHeight - fm.top
                            drawText(line, 0f, yOffset, element.paint)
                        }

                    } else {
                        element.bitmap?.let {
                            drawBitmap(it, -it.width / 2f, -it.height / 2f, null)
                        }
                    }

                    if (element.isSelected) {
                        val localContentW = element.getLocalContentWidth()
                        val localContentH = element.getLocalContentHeight()

                        val desiredScreenPadding = 10f
                        val localSpacePadding = desiredScreenPadding / element.scale

                        val desiredScreenStrokeWidth = 2f
                        val localSpaceStrokeWidth = desiredScreenStrokeWidth / element.scale

                        val dashLengthOnScreen = 10f
                        val gapLengthOnScreen = 10f
                        val localDashLength = dashLengthOnScreen / element.scale
                        val localGapLength = gapLengthOnScreen / element.scale

                        val boxPaint = Paint().apply {
                            color = Color.GRAY
                            style = Paint.Style.STROKE
                            pathEffect =
                                DashPathEffect(floatArrayOf(localDashLength, localGapLength), 0f)
                            strokeWidth = localSpaceStrokeWidth
                        }

                        drawRect(
                            (-localContentW / 2f) - localSpacePadding,
                            (-localContentH / 2f) - localSpacePadding,
                            (localContentW / 2f) + localSpacePadding,
                            (localContentH / 2f) + localSpacePadding,
                            boxPaint
                        )

                        if (!element.isLocked) {
                            val iconPositions = element.getIconPositions()

                            iconPositions.forEach { (iconName, position) ->
                                val iconBitmap = when (iconName) {
                                    "delete" -> removeIcon
                                    "rotate" -> rotateIcon
                                    "resize" -> resizeIcon
                                    "edit" -> editIcon
                                    else -> null
                                }

                                iconBitmap?.let { bmp ->
                                    val localIconDrawWidth = desiredIconScreenSizePx / element.scale
                                    val localIconDrawHeight =
                                        desiredIconScreenSizePx / element.scale // Assuming square icons

                                    val srcRect = Rect(0, 0, bmp.width, bmp.height)
                                    val dstRect = RectF(
                                        position.x - localIconDrawWidth / 2f,
                                        position.y - localIconDrawHeight / 2f,
                                        position.x + localIconDrawWidth / 2f,
                                        position.y + localIconDrawHeight / 2f
                                    )
                                    drawBitmap(bmp, srcRect, dstRect, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeElement(element: CanvasElement) {
        canvasElements.remove(element)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - offsetX) / scale
        val y = (event.y - offsetY) / scale

        val currentSelectedElement = canvasElements.find { it.isSelected }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentSelectedElement?.let { element ->
                    if (!element.isLocked) {
                        val iconPositions = element.getIconPositions()
                        val matrix = Matrix()


                        // Create transformation matrix for the element
                        matrix.postTranslate(-element.x, -element.y) // Move to origin
                        matrix.postRotate(-element.rotation) // Undo rotation
                        matrix.postScale(1f / element.scale, 1f / element.scale) // Undo scale

                        // Transform touch point to element's local coordinate system
                        val touchPoint = floatArrayOf(x, y)
                        matrix.mapPoints(touchPoint)
                        val localTouchX = touchPoint[0]
                        val localTouchY = touchPoint[1]

                        val adjustedIconHitSize = 60f / element.scale

                        val touchedIconInfo = iconPositions.entries.firstOrNull { (_, iconPos) ->
                            val dx = localTouchX - iconPos.x
                            val dy = localTouchY - iconPos.y
                            val distance = hypot(dx, dy)
                            distance < adjustedIconHitSize / 2f
                        }

                        if (touchedIconInfo != null) {
                            iconTouched = touchedIconInfo.key
                            when (iconTouched) {
                                "delete" -> {
                                    onElementRemoved?.invoke(element)
                                    removeElement(element)
                                    return true // Consume the event immediately
                                }

                                "rotate" -> {
                                    currentMode = Mode.ROTATE
                                    touchStartX = x
                                    touchStartY = y
                                    lastRotation = element.rotation
                                }

                                "resize" -> {
                                    currentMode = Mode.RESIZE
                                    touchStartX = x
                                    touchStartY = y
                                }

                                "edit" -> {
                                    onEditTextRequested?.invoke(element) // Invoke edit callback
                                    return true // Consume the touch event
                                }
                            }
                            invalidate()
                            return true
                        }
                    }
                }

                // Check topmost element under touch
                val touchedElement = canvasElements.sortedByDescending { it.zIndex }.firstOrNull {
                    val matrix = Matrix()
                    matrix.postTranslate(-it.x, -it.y)
                    matrix.postRotate(-it.rotation)
                    matrix.postScale(1f / it.scale, 1f / it.scale)

                    val touchPoint = floatArrayOf(x, y)
                    matrix.mapPoints(touchPoint)

                    val bounds = RectF(
                        -it.getLocalContentWidth() / 2f,
                        -it.getLocalContentHeight() / 2f,
                        it.getLocalContentWidth() / 2f,
                        it.getLocalContentHeight() / 2f
                    )
                    bounds.contains(touchPoint[0], touchPoint[1])
                }

                if (touchedElement != null) {
                    if (currentSelectedElement != touchedElement) {
                        onElementSelected?.invoke(touchedElement)
                        onElementSelected?.invoke(touchedElement)
                        currentMode = Mode.NONE
                        invalidate()
                    } else if (!touchedElement.isLocked) {  // Only allow drag if unlocked
                        currentMode = Mode.DRAG
                        touchStartX = x
                        touchStartY = y
                    }
                    return true
                } else {
                    if (currentSelectedElement != null) {
                        currentMode = Mode.NONE
                        invalidate()
                    }
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                currentSelectedElement?.let { element ->
                    if (element.isLocked) {
                        return true
                    }

                    when (currentMode) {
                        Mode.DRAG -> {
                            val dx = x - touchStartX
                            val dy = y - touchStartY
                            val newX = element.x + dx
                            val newY = element.y + dy

                            val bounds = element.getBounds()
                            val halfW = bounds.width() / 2
                            val halfH = bounds.height() / 2

                            val clampedX = if (canvasWidth > 2 * halfW) {
                                newX.coerceIn(halfW, canvasWidth - halfW)
                            } else {
                                newX
                            }

                            val clampedY = if (canvasHeight > 2 * halfH) {
                                newY.coerceIn(halfH, canvasHeight - halfH)
                            } else {
                                newY
                            }

                            element.x = clampedX
                            element.y = clampedY
                            checkAlignment(element)
                            touchStartX = x
                            touchStartY = y
                            onElementChanged?.invoke(element)
                            invalidate()
                        }

                        Mode.ROTATE -> {
                            val centerX = element.x
                            val centerY = element.y
                            val startAngle = atan2(touchStartY - centerY, touchStartX - centerX)
                            val currentAngle = atan2(y - centerY, x - centerX)
                            val deltaAngle =
                                Math.toDegrees((currentAngle - startAngle).toDouble()).toFloat()
                            element.rotation = (lastRotation + deltaAngle) % 360
                            invalidate()
                        }

                        Mode.RESIZE -> {
                            val centerX = element.x
                            val centerY = element.y
                            val startDist = hypot(touchStartX - centerX, touchStartY - centerY)
                            val currentDist = hypot(x - centerX, y - centerY)
                            if (startDist > 0) {
                                val scaleChange = currentDist / startDist
                                element.scale = (element.scale * scaleChange).coerceIn(0.1f, 5f)
                            }
                            touchStartX = x
                            touchStartY = y
                            invalidate()
                        }

                        Mode.NONE -> {
                            val dx = x - touchStartX
                            val dy = y - touchStartY
                            val distance = hypot(dx, dy)
                            if (distance > threshold) {
                                currentMode = Mode.DRAG
                                touchStartX = x
                                touchStartY = y
                                return true
                            }
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                showVerticalGuide = false
                showHorizontalGuide = false

                if (currentMode == Mode.DRAG || currentMode == Mode.ROTATE || currentMode == Mode.RESIZE) {
                    canvasElements.find { it.isSelected }?.let {
                        onElementChanged?.invoke(it) // Notify ViewModel of final change
                    }
                }

                iconTouched = null
                currentMode = Mode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}