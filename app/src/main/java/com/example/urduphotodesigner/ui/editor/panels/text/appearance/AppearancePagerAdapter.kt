package com.example.urduphotodesigner.ui.editor.panels.text.appearance

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.data.model.PanelTabs

class AppearancePagerAdapter(
    fragment: Fragment,
    private var tabs: List<PanelTabs>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment {
        return if (position == 0 || position == 1){
            FillStrokeFragment.newInstance(tabs[position].tab_name)
        }else{
            ShadowsFragment.newInstance(tabs[position].tab_name)
        }
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
