package com.example.urduphotodesigner.ui.editor.panels.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.model.FilterItem
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import com.example.urduphotodesigner.databinding.FragmentFiltersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FiltersFragment : Fragment() {
    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!
    private lateinit var filtersAdapter: ImageFiltersAdapter
    private val viewModel: CanvasViewModel by activityViewModels()

    // Define your list of available filters
    private val availableFilters = listOf(
        FilterItem("None", ImageFilter.None),
        FilterItem("Grayscale", ImageFilter.Grayscale),
        FilterItem("Sepia", ImageFilter.Sepia),             // Example: rotate hue by 90 degrees
        FilterItem("Invert", ImageFilter.Invert),
        FilterItem("Cool Tint", ImageFilter.CoolTint),
        FilterItem("Warm Tint", ImageFilter.WarmTint),
        FilterItem("Film", ImageFilter.Film),
        FilterItem("Teal Orange", ImageFilter.TealOrange),
        FilterItem("Black White", ImageFilter.BlackWhite),
        FilterItem("High Contrast", ImageFilter.HighContrast),
        FilterItem("Vintage", ImageFilter.Vintage)
        // Add more filters as you implement them in SizedCanvasView
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFiltersBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObservers()
    }

    private fun setupRecyclerView() {
        filtersAdapter = ImageFiltersAdapter(availableFilters) { filterItem ->
            // Get the currently selected image element from the ViewModel
            val selectedImageElement = viewModel.canvasElements.value?.firstOrNull {
                it.isSelected && it.type == ElementType.IMAGE
            }

            selectedImageElement?.let {
                viewModel.applyImageFilter(it.id, filterItem.filter)
            }
        }
        binding.filtersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filtersAdapter
        }
    }

    private fun initObservers() {
        // Observe changes to the currently selected image filter in the ViewModel
        viewModel.currentImageFilter.observe(viewLifecycleOwner) { currentFilter ->
            // Update the adapter's selected filter. This will trigger the efficient
            // notifyItemChanged calls within the adapter's setter.
            filtersAdapter.selectedFilter = currentFilter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): FiltersFragment {
            return FiltersFragment()
        }
    }
}