package com.webscare.urducanvas.ui.navigation.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.model.CanvasSize
import com.webscare.urducanvas.common.canvas.sealed.FontDownloadState
import com.webscare.urducanvas.common.utils.showGlobalSuccessSnack
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.orderWithUrduFirst
import com.webscare.urducanvas.data.model.shuffleWithUrduFirst
import com.webscare.urducanvas.databinding.FragmentPopularFontsListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PopularFontsListFragment : androidx.fragment.app.Fragment() {

    @javax.inject.Inject
    lateinit var analyticsTracker: com.webscare.urducanvas.analytics.AnalyticsTracker

    private var _binding: FragmentPopularFontsListBinding? = null
    private val binding get() = _binding
    private var shuffleAfterRefresh = false

    private val mainViewModel: com.webscare.urducanvas.viewmodels.MainViewModel by activityViewModels()
    private val filtersViewModel: com.webscare.urducanvas.viewmodels.FiltersViewModel by activityViewModels()
    private val viewModel: com.webscare.urducanvas.common.canvas.CanvasViewModel by activityViewModels()

    private lateinit var adapter: PopularFontsAdapter
    private lateinit var sglm: StaggeredGridLayoutManager
    private var filterJob: Job? = null

    private var baseFonts: List<com.webscare.urducanvas.data.model.FontEntity> = emptyList()
    private var category: String = "All"
    private var bundle: Bundle = Bundle()
    val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(category: String): PopularFontsListFragment {
            return PopularFontsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY) ?: "All"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPopularFontsListBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeData()
    }

    /**
     * Both halves of "open a font in the editor", for this screen's two routes into it.
     *
     * Same pair Home's fonts row reports: `canvas_created`, without which
     * AnalyticsTracker.startDesignWorkflow never runs and the design attempt is missing from
     * the funnel entirely, and `font_applied`, whose only other call sites are TextFragment
     * and the rows that were fixed alongside this one.
     */
    private fun reportFontCanvas(font: FontEntity, justDownloaded: Boolean) {
        analyticsTracker.logCanvasCreated(
            presetName = "font_preview",
            canvasSize = "2000x2000",
            isCustom = false,
            sourceType = com.webscare.urducanvas.analytics.AnalyticsConstants.Values.SOURCE_BLANK
        )
        analyticsTracker.logFontApplied(
            fontId = font.id.toString(),
            fontName = font.font_name,
            language = font.font_language,
            justDownloaded = justDownloaded
        )
    }

    private fun setupRecycler() {
        adapter = PopularFontsAdapter({ font, isInstalled ->
            if (!isInstalled) {
                mainViewModel.downloadFont(font)
            } else {
                // The same editor entry point Home's fonts row has, and it reported neither
                // half: canvas_created so the design workflow starts, and font_applied so a
                // font tried from this screen does not look unused. justDownloaded is false
                // by definition here — the download path is the branch above.
                reportFontCanvas(font, justDownloaded = false)
                viewModel.setCanvasSize(
                    CanvasSize(
                        id = 0,"", 2000f, 2000f
                    )
                )
                viewModel.addTextWithFont(
                    requireActivity().getString(R.string.dummyText), font, requireActivity()
                )
                view?.post { findNavController().navigate(R.id.editorFragment, bundle, navOptions) }
            }
        }, onDownload = {
            mainViewModel.downloadFont(it)
        })

        sglm = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
            isItemPrefetchEnabled = true
        }

        _binding!!.fontsRV.apply {
            layoutManager = sglm
            adapter = this@PopularFontsListFragment.adapter
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            itemAnimator = null
            setItemViewCacheSize(24)
        }

        _binding!!.swipeRefresh.setColorSchemeResources(
            R.color.appColor, R.color.black, R.color.gray
        )
        _binding!!.swipeRefresh.setOnRefreshListener {
            _binding!!.fontsRV.stopScroll()
            _binding!!.fontsRV.scrollToPosition(0)
            applyFilters(filtersViewModel.searchQuery.value, forceShuffle = true)
        }
    }

    private fun filterFonts(
        source: List<com.webscare.urducanvas.data.model.FontEntity>, category: String, query: String
    ): List<com.webscare.urducanvas.data.model.FontEntity> {
        val withoutImported = source.filter { !it.font_category.equals("Imported", true) }
        val byCategory = if (category.equals("All", true)) withoutImported
        else withoutImported.filter { it.font_category.equals(category, true) }

        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) byCategory else byCategory.filter {
            it.font_name.lowercase().contains(q)
        }
        return filtered.orderWithUrduFirst()
    }

    private fun applyFilters(query: String, forceShuffle: Boolean = false) {
        filterJob?.cancel()
        filterJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val filtered = filterFonts(baseFonts, category, query)
            val resultList = if (forceShuffle) {
                filtered.shuffleWithUrduFirst()
            } else {
                filtered
            }
            val fresh = resultList.map { it.copy() } // defensive copy for DiffUtil

            withContext(Dispatchers.Main) {
                val b = _binding ?: return@withContext
                adapter.submitList(fresh)
                sglm.invalidateSpanAssignments()
                if (!b.fontsRV.canScrollVertically(-1)) {
                    b.fontsRV.scrollToPosition(0)
                }
                b.swipeRefresh.isRefreshing = false
                renderEmptyState(isEmpty = fresh.isEmpty(), query = query)
            }
        }
    }

    /**
     * A no-match search and an empty category look identical to the user — a blank page —
     * so both get a message, and the search case names the term that found nothing.
     */
    private fun renderEmptyState(isEmpty: Boolean, query: String) {
        val b = _binding ?: return
        // The message sits over the (empty) list rather than replacing it, so pull to
        // refresh still works from the empty state.
        b.noFonts.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        val trimmed = query.trim()
        b.noFontsText.text =
            if (trimmed.isNotEmpty()) "No fonts match \"$trimmed\".\nTry another keyword."
            else getString(R.string.no_fonts_available)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                filtersViewModel.isGrid.collect { isGrid ->
                    val b = _binding ?: return@collect
                    if (isGrid) {
                        b.fontsRV.layoutManager = GridLayoutManager(requireContext(), 2)
                    } else {
                        b.fontsRV.layoutManager = LinearLayoutManager(requireContext())
                    }
                    adapter.toggleViewType(isGrid)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isLoading.collect { loading ->
                    val b = _binding ?: return@collect
                    if (!loading && b.swipeRefresh.isRefreshing) {
                        b.swipeRefresh.isRefreshing = false
                        b.fontsRV.suppressLayout(false)

                        if (shuffleAfterRefresh) {
                            shuffleAfterRefresh = false
                            applyFilters(filtersViewModel.searchQuery.value)
                        } else {
                            sglm.invalidateSpanAssignments()
                        }
                    }
                }
            }
        }

        // observe localFonts
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.localFonts.collectLatest { fonts ->
                    baseFonts = fonts
                    applyFilters(filtersViewModel.searchQuery.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.fontDownloadStates.collect { downloadState ->
                    downloadState.values.forEach { state ->
                        when (state) {
                            is FontDownloadState.Progress -> {
                                val font = state.fontEntity
                                adapter.updateProgress(
                                    font.id,
                                    _root_ide_package_.com.webscare.urducanvas.data.model.ProgressUi(
                                        progress = state.progress,
                                        isDownloading = true,
                                        isDownloaded = false
                                    )
                                )
                            }

                            is FontDownloadState.SuccessWithTypeface -> {
                                val font = state.fontEntity

                                adapter.updateProgress(
                                    font.id,
                                    _root_ide_package_.com.webscare.urducanvas.data.model.ProgressUi(
                                        100, isDownloading = false, isDownloaded = true
                                    )
                                )

                                // Consume the terminal state now, not inside the snackbar
                                // action — otherwise it stays in the StateFlow and the
                                // snackbar is replayed on every return to this screen.
                                mainViewModel.clearFontDownloadState(font.id.toString())

                                showGlobalSuccessSnack("Font downloaded") {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        // The post-download half of the same row. Left out,
                                        // the numbers would only ever have described fonts
                                        // the user already had.
                                        reportFontCanvas(font, justDownloaded = true)
                                        viewModel.setCanvasSize(
                                            CanvasSize(
                                                id = 0,"", 2000f, 2000f
                                            )
                                        )
                                        viewModel.addTextWithFont(
                                            requireActivity().getString(R.string.dummyText),
                                            font,
                                            requireActivity()
                                        )

                                        if (isAdded && findNavController().currentDestination?.id != R.id.editorFragment) {
                                            view?.post {
                                                findNavController().navigate(
                                                    R.id.editorFragment, bundle, navOptions
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is FontDownloadState.Error -> {
                                val font = state.fontEntity
                                adapter.updateProgress(
                                    font.id,
                                    _root_ide_package_.com.webscare.urducanvas.data.model.ProgressUi(
                                        progress = 0, isDownloading = false, isDownloaded = false
                                    )
                                )

                                mainViewModel.clearFontDownloadState(font.id.toString())
                                Snackbar.make(requireView(), "Download failed!", Snackbar.LENGTH_SHORT)
                                    .show()
                            }

                            else -> {}
                        }
                    }
                }
            }
        }

        // observe search query from FiltersViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                filtersViewModel.searchQuery.collectLatest { query ->
                    applyFilters(query)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        filtersViewModel.clearFilters()
    }
}