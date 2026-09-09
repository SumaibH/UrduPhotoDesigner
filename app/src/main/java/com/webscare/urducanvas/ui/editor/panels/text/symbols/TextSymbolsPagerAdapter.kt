package com.webscare.urducanvas.ui.editor.panels.text.symbols

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TextSymbolsPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    private val categories = listOf(
        SymbolCategory.UPPER,
        SymbolCategory.LOWER,
        SymbolCategory.QURANIC,
        SymbolCategory.HONORIFICS,
        SymbolCategory.ORNAMENTS
    )

    override fun getItemCount(): Int = categories.size

    override fun getItemId(position: Int): Long = categories[position].ordinal.toLong()

    override fun containsItem(itemId: Long): Boolean =
        itemId in 0 until categories.size

    override fun createFragment(position: Int): Fragment {
        return SymbolPageFragment.newInstance(categories[position])
    }
}
