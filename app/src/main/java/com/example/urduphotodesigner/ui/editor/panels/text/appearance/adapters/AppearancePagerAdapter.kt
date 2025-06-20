package com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.data.model.PanelTabs
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.BlendFragment
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.FillStrokeFragment
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.LabelsFragment
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.ShadowsFragment

class AppearancePagerAdapter(
    fragment: Fragment,
    private var tabs: List<PanelTabs>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0, 1 -> FillStrokeFragment.newInstance(tabs[position].tab_name)
            2 -> ShadowsFragment.newInstance()
            3 -> LabelsFragment.newInstance()
            4 -> BlendFragment.newInstance()
            else -> BlendFragment.newInstance()
        }
    }

    override fun getItemId(position: Int): Long {
        return tabs[position].id.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return tabs.any { it.id.toLong() == itemId }
    }
}
