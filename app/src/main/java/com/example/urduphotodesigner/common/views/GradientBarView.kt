package com.example.urduphotodesigner.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.example.urduphotodesigner.common.canvas.enums.GradientOrientation
import com.example.urduphotodesigner.common.canvas.enums.GradientType
import com.example.urduphotodesigner.common.canvas.model.GradientItem
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GradientBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var gradientItem: GradientItem = GradientItem()
        set(value) {
            field = value
            colors = value.colors.toMutableList()
            positions = value.positions.toMutableList()
            invalidateShader()
        }

    private var colors = mutableListOf<Int>()
    private var positions = mutableListOf<Float>()

    var onStopSelected: ((index: Int) -> Unit)? = null
    var onStopAdded: ((index: Int, color: Int, position: Float) -> Unit)? = null
    var onStopMoved: ((index: Int, newPosition: Float) -> Unit)? = null
    var onStopRemoved: ((index: Int) -> Unit)? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
        color = Color.WHITE
    }

    private val handleRadius = resources.displayMetrics.density * 10
    private val barCornerRadius = resources.displayMetrics.density * 12
    private val barPadding = resources.displayMetrics.density
    private val extraInset = handleRadius / 2.5f

    private var activeHandle = -1
    private var pendingHandle = -1
    private var pendingAdd = false
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        gradientItem = gradientItem
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Inset bar so handles stay fully inside
        val barLeft   = barPadding
        val barTop    = barPadding + handleRadius
        val barRight  = width  - barPadding
        val barBottom = height - barPadding - handleRadius

        // Draw gradient-filled bar background
        val rect = RectF(barLeft, barTop, barRight, barBottom)
        fillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, fillPaint)

        // Compute handle travel width (excluding both radii)
        // but also inset by extraInset on each side
        val minX = barLeft + handleRadius + extraInset
        val maxX = barRight - handleRadius - extraInset
        val effWidth = maxX - minX

        // Draw each handle
        positions.forEachIndexed { i, pos ->
            val cx = minX + (pos.coerceIn(0f, 1f) * effWidth)
            val cy = height / 2f
            fillPaint.shader = null
            fillPaint.color = colors[i]
            canvas.drawCircle(cx, cy, handleRadius, fillPaint)
            canvas.drawCircle(cx, cy, handleRadius, strokePaint)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val rawX = ev.x
        val rawY = ev.y

        // horizontal limits for a handle center
        val minX = barPadding + handleRadius + extraInset + handleRadius
        val maxX = width - (barPadding + handleRadius + extraInset + handleRadius)
        val x = rawX.coerceIn(minX, maxX)
        val effWidth = maxX - minX
        val pos = (x - minX) / effWidth

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = rawX
                downY = rawY

                val touchSize = resources.displayMetrics.density * 48
                val hitRadius = touchSize / 2f

                pendingHandle = if (rawY in (barPadding + handleRadius)..(height - barPadding - handleRadius)) {
                    positions.indexOfFirst {
                        val handleX = minX + (it * effWidth)
                        abs(handleX - rawX) < hitRadius
                    }
                } else -1

                if (pendingHandle >= 0) {
                    activeHandle = pendingHandle
                } else {
                    // only queue an add if we really clicked *away* from any handle
                    pendingAdd = true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle >= 0) {
                    // you’re dragging an existing handle
                    if (abs(rawX - downX) > touchSlop || abs(rawY - downY) > touchSlop) {
                        // normal move logic
                        if (rawY > height + handleRadius) {
                            removeHandle(activeHandle)
                            return true
                        }
                        positions[activeHandle] = pos.coerceIn(0f, 1f)
                        gradientItem.positions = positions.toList()
                        onStopMoved?.invoke(activeHandle, pos)
                        invalidateShader()
                    }
                    // once we start dragging a handle, cancel any pending add
                    pendingAdd = false
                    return true
                } else if (pendingAdd) {
                    // if user moves more than slop, they’re *not* tapping to add
                    if (abs(rawX - downX) > touchSlop || abs(rawY - downY) > touchSlop) {
                        pendingAdd = false
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeHandle >= 0) {
                    // short tap on an existing handle = select
                    if (pendingHandle == activeHandle
                        && abs(rawX - downX) <= touchSlop
                        && abs(rawY - downY) <= touchSlop
                    ) {
                        onStopSelected?.invoke(activeHandle)
                    }
                } else if (pendingAdd) {
                    // 2) only add if finger never strayed outside slop
                    if (abs(rawX - downX) <= touchSlop && abs(rawY - downY) <= touchSlop) {
                        val idx = positions.indexOfFirst { it > pos }.takeIf { it >= 0 } ?: positions.size
                        val sampled = sampleColorAt(pos)

                        onStopAdded?.invoke(idx, sampled, pos)
                        invalidateShader()
                    }
                    // either way, consume the tap and clear
                    pendingAdd = false
                    activeHandle = -1
                    pendingHandle = -1
                    return true
                }

                // fallback clean up
                activeHandle = -1
                pendingHandle = -1
                pendingAdd = false
            }
        }

        return super.onTouchEvent(ev)
    }

    private fun removeHandle(index: Int) {
        if (colors.size <= 2 || index !in colors.indices) return

        onStopRemoved?.invoke(index)
        invalidateShader()
    }

    private fun invalidateShader() {
        if (width == 0 || height == 0) return
        val barLeft   = barPadding + handleRadius
        val barRight  = width  - barPadding - handleRadius
        val extraW    = extraInset + handleRadius
        val leftEdge  = barLeft + extraW
        val rightEdge = barRight - extraW
        when (gradientItem.type) {
            GradientType.LINEAR -> {
                val w = (rightEdge - leftEdge) * gradientItem.scale
                val h = (height - 2 * (barPadding + handleRadius)) * gradientItem.scale
                val rad = Math.toRadians(gradientItem.angle.toDouble())
                val dx = (cos(rad) * w).toFloat()
                val dy = (sin(rad) * h).toFloat()
                val cx = leftEdge + (w * 0.5f)
                val cy = barPadding + handleRadius + ((height - 2 * (barPadding + handleRadius)) - h) / 2f
                fillPaint.shader = LinearGradient(
                    cx - dx/2, cy - dy/2,
                    cx + dx/2, cy + dy/2,
                    colors.toIntArray(), positions.toFloatArray(), Shader.TileMode.CLAMP
                )
            }
            GradientType.RADIAL -> {
                val cx     = width  * gradientItem.centerX
                val cy     = height * gradientItem.centerY
                val radius = min(rightEdge - leftEdge, height - 2*(barPadding+handleRadius)) *
                        gradientItem.radialRadiusFactor * gradientItem.scale
                fillPaint.shader = RadialGradient(
                    cx, cy, radius,
                    colors.toIntArray(), positions.toFloatArray(),
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.SWEEP -> {
                val cx = width  * gradientItem.centerX
                val cy = height * gradientItem.centerY
                val shader = SweepGradient(
                    cx, cy,
                    colors.toIntArray(), positions.toFloatArray()
                )
                // rotate by start‐angle
                val matrix = android.graphics.Matrix().apply {
                    setRotate(gradientItem.sweepStartAngle, cx, cy)
                }
                shader.setLocalMatrix(matrix)
                fillPaint.shader = shader
            }
        }
        invalidate()
    }

    private fun sampleColorAt(pos: Float): Int {
        val i = positions.indexOfLast { it <= pos }.coerceAtLeast(0)
        if (i == positions.lastIndex) return colors.last()
        val start = positions[i]
        val end = positions[i + 1]
        val t = (pos - start) / (end - start)
        val c0 = colors[i]
        val c1 = colors[i + 1]
        fun lerp(a: Int, b: Int) = (a + ((b - a) * t)).toInt()
        val a = lerp(Color.alpha(c0), Color.alpha(c1))
        val r = lerp(Color.red(c0), Color.red(c1))
        val g = lerp(Color.green(c0), Color.green(c1))
        val b = lerp(Color.blue(c0), Color.blue(c1))
        return Color.argb(a, r, g, b)
    }

    fun swapStops() {
        // reverse stops and rebind
        gradientItem = gradientItem.swapped()
    }

    fun setOrientation(o: GradientOrientation) {
        gradientItem = gradientItem.withOrientation(o)
    }

    fun setSweepStartAngle(deg: Float) {
        gradientItem = gradientItem.withSweepStart(deg)
    }

    fun setRadialCenter(x: Float, y: Float) {
        gradientItem = gradientItem.withRadialCenter(x, y)
    }

}
