package com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.urduphotodesigner.common.canvas.enums.BlendType
import com.example.urduphotodesigner.databinding.SpinnerItemBinding.inflate

class CustomSpinnerAdapter(
    private val items: List<BlendType>,
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = inflate(LayoutInflater.from(parent.context), parent, false)
        val blendType = items[position]
        binding.spinnerText.text = blendType.name
        return binding.root
    }
}
