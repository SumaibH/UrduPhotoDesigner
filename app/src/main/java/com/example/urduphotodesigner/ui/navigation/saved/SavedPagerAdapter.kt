package com.example.urduphotodesigner.ui.navigation.saved

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SavedPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tabs: List<String>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return SavedListFragment.newInstance(tabs[position])
    }
}