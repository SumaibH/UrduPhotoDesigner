package com.example.urduphotodesigner.ui.editor.panels.images

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ImagesPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private var tabs: List<String>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    fun setTabs(newTabs: List<String>) {
        this.tabs = newTabs
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return ImagesListFragment.newInstance(tabs[position])
    }

}