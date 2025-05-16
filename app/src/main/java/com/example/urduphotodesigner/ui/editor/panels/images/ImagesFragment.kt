package com.example.urduphotodesigner.ui.editor.panels.images

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentImagesBinding
import com.example.urduphotodesigner.ui.editor.panels.background.BackgroundPagerAdapter
import com.example.urduphotodesigner.viewmodels.MainViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImagesFragment : Fragment() {
    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private var tabs = mutableListOf<String>()
    private lateinit var adapter: ImagesPagerAdapter
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs.addAll(listOf("Image", "Color", "Gradient")) // static tabs first

        adapter = ImagesPagerAdapter(
            requireActivity().supportFragmentManager,
            lifecycle,
            tabs
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        observeCategories()
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            mainViewModel.localImages.collect { images ->
                val additionalTabs = images
                    .map { it.category.trim() }
                    .filterNot { it.equals("Background", true) || it.equals("Image", true) }
                    .distinct()

                // Add unique new categories to tabs
                tabs.clear()
                tabs.addAll(additionalTabs)

                // Update adapter and tabs
                adapter.setTabs(tabs)
                setupTabLayout()
            }
        }
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val tabView = LayoutInflater.from(context).inflate(R.layout.custom_tab, null)
            tabView.findViewById<TextView>(R.id.tabTitle).text = tabs[position]
            tab.customView = tabView
        }.attach()

        updateTabStyles(binding.tabLayout.selectedTabPosition)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabStyles(position)
            }
        })
    }

    fun updateTabStyles(selectedPosition: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tabView = binding.tabLayout.getTabAt(i)?.customView
            val root = tabView?.findViewById<ConstraintLayout>(R.id.tabRoot)
            val text = tabView?.findViewById<TextView>(R.id.tabTitle)

            if (i == selectedPosition) {
                root?.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.appColor)
                )
                text?.setTextColor(ContextCompat.getColor(requireContext(), R.color.whiteText))
            } else {
                root?.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.contrast)
                )
                text?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}