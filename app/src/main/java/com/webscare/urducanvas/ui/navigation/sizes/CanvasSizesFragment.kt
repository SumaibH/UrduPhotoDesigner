package com.webscare.urducanvas.ui.navigation.sizes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.AnalyticsConstants
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.model.CanvasSize
import com.webscare.urducanvas.common.utils.InsetUtils.applyStatusBarTopPadding
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.setupClearButton
import com.webscare.urducanvas.databinding.FragmentCanvasSizesBinding
import com.webscare.urducanvas.ui.creation.CanvasSizeAdapter
import com.webscare.urducanvas.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The full canvas-size list behind "See all".
 *
 * This used to reopen the New Canvas sheet, which carries a Custom width/height
 * block and a Create button — controls for building a size, when all this entry
 * point is for is picking one of the sizes already listed. A screen, like Popular
 * Fonts, rather than a sheet: it is a destination people search and scroll, not a
 * quick action over Home.
 */
@AndroidEntryPoint
class CanvasSizesFragment : Fragment() {

    private var _binding: FragmentCanvasSizesBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val canvasViewModel: CanvasViewModel by activityViewModels()

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private var adapter: CanvasSizeAdapter? = null

    /** Everything the database holds, before the search box narrows it. */
    private var allSizes: List<CanvasSize> = emptyList()
    private var query: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCanvasSizesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge to edge: the window no longer reserves the status bar, so leave the margin here.
        view.applyStatusBarTopPadding()
        setupList()
        setEvents()
        observeSizes()
    }

    private fun setupList() {
        adapter = CanvasSizeAdapter(
            items = emptyList(),
            onClick = { size -> openCanvas(size) },
            useNormalLayout = true
        )
        binding.sizesRV.adapter = adapter
    }

    private fun setEvents() {
        binding.back.addPressEffect { findNavController().navigateUp() }
        binding.searchBar.addTextChangedListener { text ->
            query = text.toString()
            render()
        }
        binding.searchBar.setupClearButton {
            query = ""
            render()
        }
    }

    private fun observeSizes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.localCanvasSizes.collect { entities ->
                    allSizes = entities.map {
                        CanvasSize(id = it.id, name = it.name, width = it.width, height = it.height)
                    }
                    render()
                }
            }
        }
    }

    private fun render() {
        val shown = if (query.isBlank()) allSizes else {
            val q = query.trim()
            allSizes.filter { it.name.contains(q, ignoreCase = true) }
        }
        adapter?.submitList(shown)
        _binding?.emptyState?.isVisible = shown.isEmpty() && allSizes.isNotEmpty()
    }

    private fun openCanvas(size: CanvasSize) {
        analyticsTracker.logCanvasCreated(
            presetName = size.name,
            canvasSize = "${size.width.toInt()}x${size.height.toInt()}",
            isCustom = false,
            sourceType = AnalyticsConstants.Values.SOURCE_BLANK
        )
        canvasViewModel.clearCanvas()
        canvasViewModel.setCanvasSize(size)
        // Popped on the way through, so Back from the editor lands on Home rather than
        // on the list the canvas was chosen from.
        view?.post {
            findNavController().navigate(
                R.id.editorFragment,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.canvasSizesFragment) { inclusive = true }
                }
            )
        }
    }

    override fun onDestroyView() {
        _binding?.sizesRV?.adapter = null
        adapter = null
        super.onDestroyView()
        _binding = null
    }
}
