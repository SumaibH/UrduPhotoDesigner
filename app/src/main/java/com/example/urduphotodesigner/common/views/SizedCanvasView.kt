package com.example.urduphotodesigner.common.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasElement
import com.example.urduphotodesigner.common.canvas.ElementType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class SizedCanvasView @JvmOverloads constructor(
    context: Context,
    private val canvasWidth: Int = 300,
    private val canvasHeight: Int = 300,
    attrs: AttributeSet? = null,
    var onEditTextRequested: ((CanvasElement) -> Unit)? = null,
    var onElementChanged: ((CanvasElement) -> Unit)? = null,
    var onElementRemoved: ((CanvasElement) -> Unit)? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var iconHitSize = 80f
    private var iconTouched: String? = null

    private var backgroundGradient: LinearGradient? = null
    private var backgroundImage: Bitmap? = null
    private var stickers = mutableListOf<CanvasElement>()
    private val textElements = mutableListOf<CanvasElement>()

    private val selectedElements = mutableSetOf<CanvasElement>()
    private var selectedElementForAction: CanvasElement? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var currentMode: Mode = Mode.NONE
    private var lastRotation = 0f

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private enum class Mode { NONE, DRAG, ROTATE, RESIZE }

    private val cornerIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_cross))
    }
    private val resizeIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_resize))
    }
    private val rotateIcon: Bitmap by lazy {
        drawableToBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_rotate))
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
        val currentElements = (textElements + stickers).associateBy { it.id }

        newElements.forEach { new ->
            val existing = currentElements[new.id]
            if (existing != null) {
                // Update properties
                existing.x = new.x
                existing.y = new.y
                existing.rotation = new.rotation
                existing.scale = new.scale
                existing.text = new.text
                existing.paint.set(new.paint) // Deep copy
            } else {
                // New element - add to canvas
                when (new.type) {
                    ElementType.TEXT -> {
                        val element = CanvasElement(
                            context,
                            ElementType.TEXT,
                            new.text,
                            x = new.x,
                            y = new.y,
                            id = new.id
                        )
                        element.paint.set(new.paint)
                        textElements.add(element)
                    }
                    ElementType.IMAGE -> {
                        new.bitmap?.let {
                            val element = CanvasElement(
                                context,
                                ElementType.IMAGE,
                                bitmap = it,
                                x = new.x,
                                y = new.y,
                                id = new.id
                            )
                            element.rotation = new.rotation
                            element.scale = new.scale
                            stickers.add(element)
                        }
                    }
                }
            }
        }

        // Optionally remove stale elements (not in the new list)
        val newIds = newElements.map { it.id }.toSet()
        textElements.removeAll { it.id !in newIds }
        stickers.removeAll { it.id !in newIds }

        invalidate()
    }

    fun clearCanvas() {
        stickers.clear()
        textElements.clear()
        selectedElementForAction = null
        backgroundPaint.color = Color.WHITE // Or your default background color
        backgroundGradient = null
        backgroundImage = null
        invalidate() // Redraw the canvas
    }

    fun addText(text: String, context: Context) {
        val element = CanvasElement(
            context,
            ElementType.TEXT,
            text,
            x = canvasWidth / 2f,
            y = canvasHeight / 2f
        )
        textElements.add(element)
        selectedElementForAction = element
        invalidate()
    }

    fun removeSelectedElement() {
        textElements.remove(selectedElementForAction)
        stickers.remove(selectedElementForAction)
        selectedElementForAction = null
        invalidate()
    }

    fun setFont(typeface: Typeface) {
        selectedElementForAction?.paint?.typeface = typeface
        invalidate()
    }

    fun setTextColor(color: Int) {
        selectedElementForAction?.paint?.color = color
        invalidate()
    }

    fun setTextSize(size: Float) {
        selectedElementForAction?.paint?.textSize = size
        invalidate()
    }

    fun setTextAlignment(alignment: Paint.Align) {
        selectedElementForAction?.paint?.textAlign = alignment
        invalidate()
    }

    fun setOpacity(opacity: Int) {
        selectedElementForAction?.paint?.alpha = opacity
        invalidate()
    }

    fun updateText(text: String) {
        selectedElementForAction?.text = text
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
        backgroundImage = Bitmap.createScaledBitmap(bitmap, canvasWidth, canvasHeight, true)
        backgroundGradient = null
        invalidate()
    }

    fun addSticker(bitmap: Bitmap, context: Context) {
        val maxDim = 200 // Max width/height in pixels for display
        val ratio = bitmap.width.toFloat() / bitmap.height
        val (newWidth, newHeight) = if (ratio >= 1) {
            maxDim to (maxDim / ratio).toInt()
        } else {
            (maxDim * ratio).toInt() to maxDim
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val element = CanvasElement(
            context,
            ElementType.IMAGE,
            bitmap = scaledBitmap,
            x = canvasWidth / 2f,
            y = canvasHeight / 2f
        )
        stickers.add(element)
        selectedElementForAction = element
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        val widthRatio = parentWidth.toFloat() / canvasWidth
        val heightRatio = parentHeight.toFloat() / canvasHeight
        scale = minOf(widthRatio, heightRatio)

        setMeasuredDimension(parentWidth, parentHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaledWidth = canvasWidth * scale
        val scaledHeight = canvasHeight * scale
        offsetX = (width - scaledWidth) / 2f
        offsetY = (height - scaledHeight) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage!!, 0f, 0f, null)
        } else if (backgroundGradient != null) {
            val gradientPaint = Paint().apply { shader = backgroundGradient }
            canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), gradientPaint)
        } else {
            canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), backgroundPaint)
        }

        (textElements + stickers).forEach { element ->
            canvas.save()
            canvas.translate(element.x, element.y)
            canvas.rotate(element.rotation)
            canvas.scale(element.scale, element.scale)

            if (element.type == ElementType.TEXT) {
                val fm = element.paint.fontMetrics
                val textHeight = fm.descent - fm.ascent
                canvas.drawText(element.text, 0f, -fm.ascent - textHeight / 2f, element.paint)

            } else {
                element.bitmap?.let {
                    canvas.drawBitmap(it, -it.width / 2f, -it.height / 2f, null)
                }
            }

            if (element == selectedElementForAction) {
                val bounds = element.getBounds()
                val boxPaint = Paint().apply {
                    color = Color.GRAY
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                    strokeWidth = 2f
                }
                canvas.drawRect(
                    -bounds.width() / 2,
                    -bounds.height() / 2,
                    bounds.width() / 2,
                    bounds.height() / 2,
                    boxPaint
                )

                val iconPositions = element.getIconPositions()

                iconPositions["delete"]?.let {
                    canvas.drawBitmap(
                        cornerIcon,
                        it.x - cornerIcon.width / 2,
                        it.y - cornerIcon.height / 2,
                        null
                    )
                }
                iconPositions["rotate"]?.let {
                    canvas.drawBitmap(
                        rotateIcon,
                        it.x - rotateIcon.width / 2,
                        it.y - rotateIcon.height / 2,
                        null
                    )
                }
                iconPositions["resize"]?.let {
                    canvas.drawBitmap(
                        resizeIcon,
                        it.x - resizeIcon.width / 3,
                        it.y - resizeIcon.height / 3,
                        null
                    )
                }
            }

            canvas.restore()
        }

        canvas.restore()
    }

    private fun removeElement(element: CanvasElement) {
        textElements.remove(element)
        stickers.remove(element)
        selectedElements.remove(element)
        invalidate()  // Redraw the view
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - offsetX) / scale
        val y = (event.y - offsetY) / scale

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedElementForAction?.let { element ->
                    val iconPositions = element.getIconPositions()
                    val localX = x - element.x
                    val localY = y - element.y

                    val touchedIcon = iconPositions.entries.firstOrNull { (_, pos) ->
                        abs(localX - pos.x) < iconHitSize / 2 && abs(localY - pos.y) < iconHitSize / 2
                    }?.key

                    if (touchedIcon != null) {
                        iconTouched = touchedIcon
                        currentMode = when (touchedIcon) {
                            "delete" -> Mode.NONE
                            "rotate" -> Mode.ROTATE
                            "resize" -> Mode.RESIZE
                            else -> Mode.NONE
                        }
                        touchStartX = x
                        touchStartY = y
                        return true
                    }
                }

                // Check topmost element under touch
                val touchedElement =
                    (textElements + stickers).lastOrNull { it.getBounds().contains(x, y) }

                if (touchedElement != null) {
                    if (touchedElement == selectedElementForAction) {
                        // Already selected - start drag
                        currentMode = Mode.DRAG
                        touchStartX = x
                        touchStartY = y
                    } else {
                        // Select new element
                        selectedElementForAction = touchedElement
                        currentMode = Mode.NONE
                    }
                    invalidate()
                    return true
                } else {
                    // Click outside all elements - deselect
                    if (selectedElementForAction != null) {
                        selectedElementForAction = null
                        invalidate()
                    }
                    currentMode = Mode.NONE
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                selectedElementForAction?.let { element ->
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

                            onElementChanged?.invoke(element)
                            touchStartX = x
                            touchStartY = y
                            invalidate()
                        }

                        Mode.ROTATE -> {
                            val centerX = element.x
                            val centerY = element.y
                            val startAngle = atan2(touchStartY - centerY, touchStartX - centerX)
                            val currentAngle = atan2(y - centerY, x - centerX)
                            val deltaAngle =
                                Math.toDegrees((currentAngle - startAngle).toDouble()).toFloat()
                            element.rotation += deltaAngle
                            onElementChanged?.invoke(element)
                            touchStartX = x
                            touchStartY = y
                            invalidate()
                        }

                        Mode.RESIZE -> {
                            val centerX = element.x
                            val centerY = element.y
                            val startDist = hypot(touchStartX - centerX, touchStartY - centerY)
                            val currentDist = hypot(x - centerX, y - centerY)
                            val scaleChange = currentDist / startDist
                            element.scale = (element.scale * scaleChange).coerceIn(0.1f, 3f)
                            onElementChanged?.invoke(element)
                            touchStartX = x
                            touchStartY = y
                            invalidate()
                        }

                        else -> {}
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (iconTouched == "delete") {
                    selectedElementForAction?.let {
                        onElementRemoved?.invoke(it)
                        removeElement(it)
                    }
                    iconTouched = null
                    currentMode = Mode.NONE
                    return true
                }

                // If clicked on selected element but NOT on icon, trigger edit callback
                selectedElementForAction?.let { element ->
                    val bounds = element.getBounds()
                    if (bounds.contains(x, y) && iconTouched == null) {
                        if (element.type == ElementType.TEXT) { // Only for text
                            onEditTextRequested?.invoke(element)
                        }
                        currentMode = Mode.NONE
                        return true
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