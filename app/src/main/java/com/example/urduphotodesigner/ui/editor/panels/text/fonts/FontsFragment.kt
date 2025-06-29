package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.urduphotodesigner.data.model.FontCategory
import com.example.urduphotodesigner.databinding.FragmentFontsBinding
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FontsFragment : Fragment() {
    private var _binding: FragmentFontsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FontCategoryAdapter
    private lateinit var categories: ArrayList<FontCategory>
    private lateinit var pagerAdapter: FontsPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        initObservers()
    }

    private fun setupRecyclerViews() {
        categories = ArrayList()
        adapter = FontCategoryAdapter { font ->
            handleFontSelection(font)
        }
        binding.categories.adapter = adapter

        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        pagerAdapter = FontsPagerAdapter(this@FontsFragment, categories)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selectedCategory = categories[position]
                handleFontSelection(selectedCategory)
                binding.categories.smoothScrollToPosition(position)
            }
        })

    }

    private fun initObservers() {
        lifecycleScope.launch {
            mainViewModel.localFonts.collect { fonts ->
                val distinctCategories = fonts.map { it.font_category }.distinct()
                val newCategories = mutableListOf<FontCategory>()

                // Add "All" category at the top
                newCategories.add(FontCategory(id = -1, font_category = "All", is_selected = true))

                // Add actual categories
                distinctCategories.forEachIndexed { index, category ->
                    newCategories.add(FontCategory(id = index, font_category = category, is_selected = false))
                }

                if (newCategories != categories) {
                    categories.clear()
                    categories.addAll(newCategories)
                    adapter.submitList(ArrayList(categories))
                    pagerAdapter.updateCategories(categories)
                    handleFontSelection(categories.firstOrNull())
                }
            }
        }
    }

    private fun handleFontSelection(selectedCategory: FontCategory?) {
        selectedCategory?.let { category ->
            val selectedIndex = categories.indexOfFirst { it.font_category == category.font_category }

            // Update selected item visuals
            val updatedCategories = categories.map {
                it.copy(is_selected = it.font_category == category.font_category)
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
        fun newInstance(): FontsFragment {
            return FontsFragment()
        }
    }
}