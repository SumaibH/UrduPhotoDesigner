package com.webscare.urducanvas.common.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

/**
 * A SeekBar whose minimum works below API 26.
 *
 * `AbsSeekBar.setMin` arrived in API 26 and this app ships to API 24, so every slider that
 * set a minimum — text shadow distance and radius, glow, blend, shape stroke width, image
 * stroke and shadow, the tone and clarity adjustments — threw `NoSuchMethodError` the
 * moment its panel opened on Android 7. The panels were unreachable there, not merely
 * mis-scaled, and nothing in the build caught it because the call compiles fine.
 *
 * On API 26 and up this defers entirely to the platform. Below that it keeps the real
 * minimum itself and runs the underlying bar over `0..(max - min)`, translating on the way
 * in and out — including the `progress` handed to the change listener, which is why call
 * sites need no arithmetic of their own and read exactly as they did before.
 */
class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.seekBarStyle
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    /** Real minimum while emulating; always 0 when the platform handles it. */
    private var offset = 0

    /** Real maximum while emulating, so [setMin] can re-derive the underlying span. */
    private var realMax = 100

    override fun setMin(min: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setMin(min)
            return
        }
        val current = progress
        offset = min
        super.setMax((realMax - offset).coerceAtLeast(0))
        progress = current.coerceAtLeast(offset)
    }

    override fun getMin(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) super.getMin() else offset

    override fun setMax(max: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setMax(max)
            return
        }
        realMax = max
        super.setMax((realMax - offset).coerceAtLeast(0))
    }

    override fun getMax(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) super.getMax() else realMax

    override fun setProgress(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setProgress(progress)
            return
        }
        super.setProgress((progress - offset).coerceIn(0, super.getMax()))
    }

    override fun getProgress(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) super.getProgress()
        else super.getProgress() + offset

    /**
     * Wraps the caller's listener so it is handed progress in the real range.
     *
     * Without this the framework would report the underlying `0..max-min` value and every
     * call site would silently read low by the minimum.
     */
    override fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || listener == null) {
            super.setOnSeekBarChangeListener(listener)
            return
        }
        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                listener.onProgressChanged(seekBar, progress + offset, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                listener.onStartTrackingTouch(seekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                listener.onStopTrackingTouch(seekBar)
            }
        })
    }
}
