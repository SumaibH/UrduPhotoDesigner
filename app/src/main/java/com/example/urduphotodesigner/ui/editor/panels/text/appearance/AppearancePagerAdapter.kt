package com.example.urduphotodesigner.ui.editor.panels.text.appearance

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.urduphotodesigner.data.model.AppearanceTabs

class AppearancePagerAdapter(
    fragment: Fragment,
    private var tabs: List<AppearanceTabs>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment {
        return ColorsListFragment.newInstance(tabs[position].tab_name)
    }

    fun updateTabs(newTabs: List<AppearanceTabs>) {
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
