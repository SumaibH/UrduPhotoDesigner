package com.example.urduphotodesigner.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
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
            angle = value.angle
            scaleFactor = value.scale
        }

    var colors: MutableList<Int> = mutableListOf()
        private set
    var positions: MutableList<Float> = mutableListOf()
        private set
    var angle: Float = 0f
        private set
    var scaleFactor: Float = 1f
        private set

    /** Called when user taps on an existing handle */
    var onStopSelected: ((index: Int) -> Unit)? = null

    /** Called when user adds a new stop */
    var onStopAdded: ((index: Int, color: Int, position: Float) -> Unit)? = null

    /** Called when user drags a handle */
    var onStopMoved: ((index: Int, newPosition: Float) -> Unit)? = null

    // paints, sizing, etc...
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2
        color = Color.WHITE
    }
    private val handleRadius = resources.displayMetrics.density * 10
    private var activeHandle = -1

    init {
        // initialize view‐state from default gradientItem
        gradientItem = gradientItem
    }

    private fun invalidateShader() {
        if (width == 0 || height == 0) return

        // always build from our local colors/positions
        when (gradientItem.type) {
            GradientType.LINEAR -> {
                val w = width * scaleFactor
                val h = height * scaleFactor
                val rad = Math.toRadians(angle.toDouble())
                val dx = (cos(rad) * w).toFloat()
                val dy = (sin(rad) * h).toFloat()
                val cx = (width - w) / 2f
                val cy = (height - h) / 2f

                fillPaint.shader = LinearGradient(
                    cx, cy,
                    cx + dx, cy + dy,
                    colors.toIntArray(),
                    positions.toFloatArray(),
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.RADIAL -> {
                val cx = width / 2f
                val cy = height / 2f
                val radius = min(width, height) * gradientItem.radialRadiusFactor * scaleFactor

                fillPaint.shader = RadialGradient(
                    cx, cy, radius,
                    colors.toIntArray(),
                    positions.toFloatArray(),
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.SWEEP -> {
                val cx = width / 2f
                val cy = height / 2f

                fillPaint.shader = SweepGradient(
                    cx, cy,
                    colors.toIntArray(),
                    positions.toFloatArray()
                )
            }
        }
        invalidate()
    }

    private fun sampleColorAt(pos: Float): Int {
        // find which segment pos lies in
        val i = positions.indexOfLast { it <= pos }.coerceAtLeast(0)
        if (i == positions.lastIndex) return colors.last()

        val startP = positions[i]
        val endP = positions[i+1]
        val t = (pos - startP) / (endP - startP)
        val c0 = colors[i]
        val c1 = colors[i+1]

        // linear interpolate ARGB
        fun lerp(a: Int, b: Int) = (a + ((b - a) * t)).toInt()
        val a = lerp(Color.alpha(c0), Color.alpha(c1))
        val r = lerp(Color.red(c0),   Color.red(c1))
        val g = lerp(Color.green(c0), Color.green(c1))
        val b = lerp(Color.blue(c0),  Color.blue(c1))
        return Color.argb(a, r, g, b)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw gradient
        fillPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

        // draw handles
        positions.forEachIndexed { i, pos ->
            val cx = pos * width
            fillPaint.shader = null
            fillPaint.color = colors[i]
            canvas.drawCircle(cx, height / 2f, handleRadius, fillPaint)
            canvas.drawCircle(cx, height / 2f, handleRadius, strokePaint)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x.coerceIn(0f, width.toFloat())
        val pos = x / width

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // did we tap a handle?
                activeHandle = positions.indexOfFirst { abs(it * width - x) < handleRadius }
                if (activeHandle >= 0) {
                    onStopSelected?.invoke(activeHandle)
                    return true
                }

                // else: add a new stop
                val insertIndex = positions.indexOfFirst { it > pos }.takeIf { it >= 0 } ?: positions.size
                val sampledColor = sampleColorAt(pos)

                colors.add(insertIndex, sampledColor)
                positions.add(insertIndex, pos)
                // sync back to model if you like:
                gradientItem.colors = colors.toList()
                gradientItem.positions = positions.toList()

                onStopAdded?.invoke(insertIndex, sampledColor, pos)
                invalidateShader()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle >= 0) {
                    val newPos = pos.coerceIn(0f, 1f)
                    positions[activeHandle] = newPos
                    gradientItem.positions = positions.toList()
                    onStopMoved?.invoke(activeHandle, newPos)
                    invalidateShader()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = -1
            }
        }
        return super.onTouchEvent(ev)
    }
}