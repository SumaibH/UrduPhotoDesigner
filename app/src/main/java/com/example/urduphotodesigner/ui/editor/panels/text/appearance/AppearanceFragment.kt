package com.example.urduphotodesigner.ui.editor.panels.text.appearance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.urduphotodesigner.data.model.AppearanceTabs
import com.example.urduphotodesigner.databinding.FragmentAppearanceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppearanceFragment : Fragment() {
    private var _binding: FragmentAppearanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabs: ArrayList<AppearanceTabs>
    private lateinit var adapter: AppearanceAdapter
    private lateinit var pagerAdapter: AppearancePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppearanceBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        initObservers()
    }

    private fun setupRecyclerViews() {
        tabs = ArrayList()
        adapter = AppearanceAdapter { tab ->
            handleFontSelection(tab)
        }
        binding.categories.adapter = adapter

        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.offscreenPageLimit = 1

        pagerAdapter = AppearancePagerAdapter(this, tabs)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selectedCategory = tabs[position]
                handleFontSelection(selectedCategory)
                binding.categories.smoothScrollToPosition(position)
            }
        })

    }

    private fun initObservers() {
        lifecycleScope.launch {
            tabs.add(AppearanceTabs(0, "Text", true))
            tabs.add(AppearanceTabs(1, "Border", false))
            tabs.add(AppearanceTabs(2, "Shadow", false))
            tabs.add(AppearanceTabs(3, "Label", false))

            adapter.submitList(ArrayList(tabs))
            handleFontSelection(tabs.firstOrNull()) // Select "All" by default
        }
    }

    private fun handleFontSelection(selectedCategory: AppearanceTabs?) {
        selectedCategory?.let { tab ->
            val selectedIndex = tabs.indexOfFirst { it.tab_name == tab.tab_name }

            // Update selected item visuals
            val updatedCategories = tabs.map {
                it.copy(is_selected = it.tab_name == tab.tab_name)
            }
            adapter.submitList(updatedCategories)

            // Switch ViewPager page
            binding.viewPager.setCurrentItem(selectedIndex, true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AppearanceFragment {
            return AppearanceFragment()
        }
    }
}