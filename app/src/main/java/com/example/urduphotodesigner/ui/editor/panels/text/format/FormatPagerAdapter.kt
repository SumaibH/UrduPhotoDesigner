package com.example.urduphotodesigner.ui.editor.panels.text.format

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.data.model.PanelTabs
import com.example.urduphotodesigner.ui.editor.panels.text.para.ParagraphOptionsFragment

class FormatPagerAdapter(
    fragment: Fragment,
    private var tabs: List<PanelTabs>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment {
        return FormattingFragment.newInstance(tabs[position].tab_name)
    }

    fun updateTabs(newTabs: List<PanelTabs>) {
        tabs = newTabs
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return tabs[position].id.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return tabs.any { it.id.toLong() == itemId }
    }
}
