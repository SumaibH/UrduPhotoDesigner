package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.data.model.FontCategory

class FontsPagerAdapter(
    fragment: Fragment,
    private val categories: List<FontCategory>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return FontsListFragment.newInstance(categories[position].font_category)
    }
}
