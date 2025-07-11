package com.example.urduphotodesigner.ui.editor.panels.text

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.AppearanceFragment
import com.example.urduphotodesigner.ui.editor.panels.text.fonts.FontsFragment
import com.example.urduphotodesigner.ui.editor.panels.text.format.FormatFragment

class TextPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tabs: List<String>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return when (tabs[position]) {
            "Font" -> FontsFragment.newInstance()
            "Appearance" -> AppearanceFragment.newInstance()
            "Format" -> FormatFragment.newInstance()
            "Style" -> FormatFragment.newInstance()
            else -> FontsFragment.newInstance()
        }
    }

}