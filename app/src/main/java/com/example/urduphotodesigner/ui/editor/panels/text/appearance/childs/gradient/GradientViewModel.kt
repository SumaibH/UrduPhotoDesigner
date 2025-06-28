package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.urduphotodesigner.common.canvas.enums.GradientType
import com.example.urduphotodesigner.common.canvas.model.GradientItem

class GradientViewModel : ViewModel() {

    // The “source of truth” for your gradient
    private val _gradient = MutableLiveData<GradientItem>(
        GradientItem(
            colors = listOf(Color.RED, Color.YELLOW),
            positions = listOf(0f, 1f),
            angle = 0f,
            scale = 1f,
            type = GradientType.LINEAR
        )
    )
    val gradient: LiveData<GradientItem> = _gradient

    // Which stop is currently “selected” for editing?
    // null = none (e.g. user tapped empty bar)
    private val _selectedStopIndex = MutableLiveData<Int?>(null)
    val selectedStopIndex: LiveData<Int?> = _selectedStopIndex

    // —— Public API —— //

    /** Call when the user taps on an empty spot and you want to add a stop */
    fun addStop(position: Float, sampledColor: Int) {
        val item = _gradient.value ?: return
        val (c, p) = insertAt(item, position to sampledColor)
        _gradient.value = item.copy(colors = c, positions = p)
        // auto-select new stop
        _selectedStopIndex.value = c.indexOf(sampledColor)
    }

    /** Call when the user drags a handle to a new position */
    fun moveStop(index: Int, newPosition: Float) {
        val item = _gradient.value ?: return
        val c = item.colors.toMutableList()
        val p = item.positions.toMutableList()
        if (index in p.indices) {
            p[index] = newPosition.coerceIn(0f,1f)
            _gradient.value = item.copy(colors = c, positions = p)
        }
    }

    /** Call when the user taps an existing handle */
    fun selectStop(index: Int) {
        _selectedStopIndex.value = index
    }

    /** Call after the color‐picker fragment returns a new color */
    fun updateSelectedStopColor(newColor: Int) {
        val idx = _selectedStopIndex.value ?: return
        val item = _gradient.value ?: return
        val c = item.colors.toMutableList()
        if (idx in c.indices) {
            c[idx] = newColor
            _gradient.value = item.copy(colors = c)
        }
    }

    /** Adjust gradient angle (in degrees) */
    fun setAngle(deg: Float) {
        val item = _gradient.value ?: return
        _gradient.value = item.copy(angle = deg)
    }

    /** Adjust overall scale (0…1+) */
    fun setScale(scale: Float) {
        val item = _gradient.value ?: return
        _gradient.value = item.copy(scale = scale)
    }

    /** Switch between LINEAR / RADIAL / SWEEP */
    fun setType(type: GradientType) {
        val item = _gradient.value ?: return
        _gradient.value = item.copy(type = type)
    }

    // —— Helpers —— //

    private fun insertAt(
        item: GradientItem,
        newEntry: Pair<Float,Int>
    ): Pair<List<Int>,List<Float>> {
        val (pos, color) = newEntry
        val c = item.colors.toMutableList()
        val p = item.positions.toMutableList()
        val idx = p.indexOfFirst { it > pos }.takeIf { it >= 0 } ?: p.size
        c.add(idx, color)
        p.add(idx, pos.coerceIn(0f,1f))
        return c to p
    }
}