package com.example.urduphotodesigner.ui.editor.panels.text

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.colors.ColorsListFragment
import com.example.urduphotodesigner.ui.editor.panels.text.fonts.FontsListFragment
import com.example.urduphotodesigner.ui.editor.panels.text.para.ParagraphOptionsFragment

class TextPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tabs: List<String>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return when (tabs[position]) {
            "Fonts" -> FontsListFragment.newInstance()
            "Color" -> ColorsListFragment.newInstance()
            "Paragraph" -> ParagraphOptionsFragment.newInstance()
            else -> FontsListFragment.newInstance()
        }
    }

}