package com.webscare.urducanvas.ui.editor.panels.text.threed.childs

import com.webscare.urducanvas.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.enums.ElementType
import com.webscare.urducanvas.common.canvas.model.Text3DData
import com.webscare.urducanvas.common.canvas.model.Text3DPreset
import com.webscare.urducanvas.data.model.PresetCategory
import com.webscare.urducanvas.data.model.TextStylePreset
import com.webscare.urducanvas.ui.editor.panels.preview.showPresetPreview
import com.webscare.urducanvas.data.repository.TextStylesRepository
import com.webscare.urducanvas.databinding.Fragment3dPresetsBinding
import com.webscare.urducanvas.ui.editor.panels.text.styles.TextStylesGridAdapter
import com.webscare.urducanvas.viewmodels.SearchScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

/**
 * The 3D family of the Styles library, in the 3D panel. The Styles panel loads every
 * category; this one loads the THREE_D category only, plus the handful of built-in looks
 * that pick a material surface, and renders them through the same thumbnail grid so a
 * preset looks identical in both places.
 */
@AndroidEntryPoint
class Presets3DFragment : Fragment() {

    private var _binding: Fragment3dPresetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private val mainViewModel: com.webscare.urducanvas.viewmodels.MainViewModel by activityViewModels()
    private lateinit var adapter: TextStylesGridAdapter

    /** Unfiltered grid contents, so clearing the search restores them. */
    private var allPresets: List<TextStylePreset> = emptyList()

    /**
     * Extra words a preset can be found by, keyed by preset id.
     *
     * [Text3DPreset] carries a `title` and a `desc` ("Cool grey gradient face, mirror-bright
     * bevel, short depth.") that [asStylePreset] has to drop, because `TextStylePreset` has
     * nowhere to put them. They are the most descriptive text these eight built-ins have, so
     * searching "mirror" or "bevel" ought to find Chrome — this keeps them reachable without
     * changing the shared preset model.
     */
    private var extraKeywords: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment3dPresetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "None" stays at the head of the list — it is the only way back to flat text
        // once a preset has been applied.
        val builtIns = Text3DData.PRESETS.map { it.asStylePreset() }
        // Drop the styles panel's synthetic None — this grid already leads with its own.
        val library = TextStylesRepository.getPresetsByCategory(
            PresetCategory.THREE_D, requireContext()
        ).filterNot { it.id == com.webscare.urducanvas.data.model.TextStylePreset.NONE_ID }

        allPresets = builtIns + library
        extraKeywords = Text3DData.PRESETS.associate { p ->
            (BUILT_IN_PREFIX + p.id) to "${p.title} ${p.desc}".lowercase()
        }

        fun apply(preset: TextStylePreset) {
            val builtInId = preset.id.removePrefix(BUILT_IN_PREFIX)
            if (builtInId != preset.id) {
                viewModel.apply3DPreset(builtInId)
                viewModel.selectedStylePresetId.value = preset.id
            } else {
                viewModel.apply3DStylePreset(preset)
            }
        }

        adapter = TextStylesGridAdapter(
            allPresets,
            onPreviewRequested = { preset ->
                showPresetPreview(
                    preset = preset,
                    breadcrumb = getString(R.string.presets),
                    // This grid is only ever drawn in an open adjustments panel.
                    expanded = true,
                    typeface = adapter.currentTypeface,
                    fontKey = adapter.currentFontKey,
                    primaryLabel = getString(R.string.preview_use_on_canvas)
                ) { picked -> apply(picked) }
            }
        ) { preset -> apply(preset) }
        adapter.selectedPresetId = viewModel.selectedStylePresetId.value

        binding.rvPresets.layoutManager =
            GridLayoutManager(requireContext(), resources.getInteger(R.integer.panel_preset_grid_rows), RecyclerView.HORIZONTAL, false)
        binding.rvPresets.adapter = adapter

        initObservers()
    }

    private fun initObservers() {
        viewModel.selectedStylePresetId.observe(viewLifecycleOwner) { selectedId ->
            adapter.selectedPresetId = selectedId
        }

        // Thumbnails are drawn in the element's own font, the same as the Styles panel.
        viewModel.selectedElements.observe(viewLifecycleOwner) { elements ->
            val firstText = elements?.firstOrNull { it.type == ElementType.TEXT }
            val typeface = firstText?.paint?.typeface
            val fontKey = firstText?.fontId ?: firstText?.fontUrl ?: typeface?.hashCode()?.toString()
            adapter.updateTypeface(typeface, fontKey)
        }

        // The 3D tab's own query — the header icon is live on this tab now, and this is the
        // page it filters. The other four 3D sub-tabs are sliders and swatches with nothing
        // a word could match, so they are left alone.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.queryDebouncedFor(SearchScope.TEXT_3D).collect { query ->
                    if (_binding == null) return@collect
                    val filtered = filterPresets(query)
                    adapter.submitPresets(filtered)
                    // Same discipline as the Styles grid: only the page actually on screen
                    // may report a count for the shared search dialog to read back.
                    if (query.isNotBlank() && isResumed) {
                        mainViewModel.reportSearchResultCount(query, filtered.size)
                    }
                }
            }
        }
    }

    /**
     * Matches the preset name, its category label, and — for the eight built-ins — the
     * title and description that do not survive the mapping into [TextStylePreset].
     */
    private fun filterPresets(queryRaw: String): List<TextStylePreset> {
        val query = queryRaw.trim().lowercase()
        if (query.isEmpty()) return allPresets
        return allPresets.filter { preset ->
            preset.name.lowercase().contains(query) ||
                preset.category.displayName.lowercase().contains(query) ||
                extraKeywords[preset.id]?.contains(query) == true
        }
    }

    override fun onDestroyView() {
        _binding?.rvPresets?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = Presets3DFragment()

        /** Marks the entries that drive [CanvasViewModel.apply3DPreset] rather than a style. */
        const val BUILT_IN_PREFIX = "t3d_"
    }
}

/**
 * The built-in 3D looks described as a style preset, purely so the shared thumbnail
 * renderer can draw them next to the library entries. Nothing here is applied — a tap on
 * one of these still goes through [CanvasViewModel.apply3DPreset].
 */
private fun Text3DPreset.asStylePreset(): TextStylePreset {
    val front = runCatching { frontColor.toColorInt() }.getOrNull()
    val side = runCatching { extrusionColor.toColorInt() }.getOrNull()
    val glowColor = glow?.let { runCatching { it.toColorInt() }.getOrNull() }
    return TextStylePreset(
        id = Presets3DFragment.BUILT_IN_PREFIX + id,
        name = label,
        category = PresetCategory.THREE_D,
        textColor = front,
        has3dExtrude = depth > 0f,
        extrudeColor = side,
        extrudeDepth = depth,
        extrudeDx = 0.7f,
        extrudeDy = 0.7f,
        hasBevel = bevel > 0f,
        bevelDepth = bevel,
        hasOuterGlow = glowColor != null,
        outerGlowColor = glowColor,
        outerGlowRadius = 14f,
        shadowColor = if (shadowOpacityPercent > 0f) android.graphics.Color.BLACK else null,
        shadowRadius = (depth * 0.45f).coerceIn(0f, 26f),
        shadowDx = (depth * 0.35f).coerceIn(0f, 18f),
        shadowDy = (depth * 0.35f).coerceIn(0f, 18f),
        shadowOpacity = (shadowOpacityPercent * 2.55f).toInt().coerceIn(0, 255)
    )
}
