package com.example.urduphotodesigner.ui.editor.panels.layers

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.common.canvas.model.CanvasElement

class LayersPagerAdapter(
    fragment: Fragment,
    private var layers: List<CanvasElement>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = layers.size

    override fun createFragment(position: Int): Fragment {
        return LayerDetailsFragment.newInstance(layers[position].id)
    }

    override fun getItemId(position: Int): Long {
        return layers[position].id.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return layers.any { it.id.toLong() == itemId }
    }
}