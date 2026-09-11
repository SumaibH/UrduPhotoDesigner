package com.webscare.urducanvas.ui.editor.panels.text.styles

import com.webscare.urducanvas.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.data.model.PresetCategory
import com.webscare.urducanvas.data.model.TextStylePreset
import com.webscare.urducanvas.ui.editor.panels.preview.showPresetPreview
import com.webscare.urducanvas.data.repository.TextStylesRepository
import com.webscare.urducanvas.databinding.FragmentTextStyleGridBinding
import com.webscare.urducanvas.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class TextStyleGridFragment : Fragment() {

    private var _binding: FragmentTextStyleGridBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var categoryName: String = ""
    private var isAddMode: Boolean = false

    /** Unfiltered page contents, so clearing the search restores them. */
    private var allPresets: List<TextStylePreset> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = arguments?.getString(ARG_CATEGORY) ?: ""
        isAddMode = arguments?.getBoolean(ARG_IS_ADD_MODE, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextStyleGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGrid()
    }

    private fun setupGrid() {
        val category = PresetCategory.values().firstOrNull { it.name == categoryName } ?: PresetCategory.THREE_D
        val presets = TextStylesRepository.getPresetsByCategory(category, requireContext())
        allPresets = presets
        fun apply(preset: TextStylePreset) {
            if (isAddMode) {
                viewModel.addTextWithStyle("Your Text", preset, requireContext())
            } else {
                viewModel.applyTextStylePreset(preset)
            }
        }

        lateinit var adapter: TextStylesGridAdapter
        adapter = TextStylesGridAdapter(
            presets,
            onPreviewRequested = { preset ->
                showPresetPreview(
                    preset = preset,
                    breadcrumb = categoryName,
                    // This grid is only ever drawn in an open adjustments panel.
                    expanded = true,
                    typeface = adapter.currentTypeface,
                    fontKey = adapter.currentFontKey,
                    primaryLabel = getString(
                        if (isAddMode) R.string.preview_add_to_canvas
                        else R.string.preview_use_on_canvas
                    )
                ) { picked -> apply(picked) }
            }
        ) { preset -> apply(preset) }
        adapter.selectedPresetId = viewModel.selectedStylePresetId.value

        viewModel.selectedStylePresetId.observe(viewLifecycleOwner) { selectedId ->
            adapter.selectedPresetId = selectedId
        }

        viewModel.selectedElements.observe(viewLifecycleOwner) { elements ->
            val firstTextElement = elements?.firstOrNull { it.type == com.webscare.urducanvas.common.canvas.enums.ElementType.TEXT }
            val customTypeface = firstTextElement?.paint?.typeface
            val fontKey = firstTextElement?.fontId ?: firstTextElement?.fontUrl ?: customTypeface?.hashCode()?.toString()
            adapter.updateTypeface(customTypeface, fontKey)
        }

        binding.presetsGrid.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.panel_preset_grid_rows), RecyclerView.HORIZONTAL, false)
        binding.presetsGrid.adapter = adapter

        // The header's search box applies to whichever tab is open, so the
        // Styles pages filter themselves the same way the Font list does.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.queryDebounced.collect { query ->
                    if (_binding == null) return@collect
                    val filtered = filterPresets(query)
                    adapter.submitPresets(filtered)
                    // The shared search dialog has no list of its own to count. "None" is
                    // always kept by filterPresets and is not a result, so it comes off here.
                    //
                    // isResumed keeps the offscreen category pages out of it: there is one
                    // of these per preset category, this collector runs at STARTED, and
                    // every created page was writing its own count for the same query.
                    if (query.isNotBlank() && isResumed) {
                        mainViewModel.reportSearchResultCount(
                            query,
                            (filtered.size - 1).coerceAtLeast(0)
                        )
                    }
                }
            }
        }

        binding.presetsGrid.post {
            if (_binding != null) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * "None" always stays: it is the way to clear the style, not a preset, and
     * dropping it would leave a filtered page with no way back.
     */
    private fun filterPresets(queryRaw: String): List<TextStylePreset> {
        val query = queryRaw.trim().lowercase()
        if (query.isEmpty()) return allPresets
        return allPresets.filter { preset ->
            preset.id == TextStylePreset.NONE_ID ||
                    preset.name.lowercase().contains(query) ||
                    preset.category.displayName.lowercase().contains(query)
        }
    }

    override fun onDestroyView() {
        _binding?.presetsGrid?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_IS_ADD_MODE = "arg_is_add_mode"

        fun newInstance(category: PresetCategory, isAddMode: Boolean = false): TextStyleGridFragment {
            val fragment = TextStyleGridFragment()
            val args = Bundle().apply {
                putString(ARG_CATEGORY, category.name)
                putBoolean(ARG_IS_ADD_MODE, isAddMode)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
