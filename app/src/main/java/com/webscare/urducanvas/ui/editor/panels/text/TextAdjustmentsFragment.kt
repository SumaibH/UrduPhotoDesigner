package com.webscare.urducanvas.ui.editor.panels.text

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.model.ExpansionDepth
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.ui.editor.panels.text.symbols.CalligraphyBreakdownBottomSheet
import com.webscare.urducanvas.common.utils.setupPanelTabs
import com.webscare.urducanvas.common.utils.setTabEdited
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.webscare.urducanvas.databinding.FragmentTextAdjustmentsBinding
import com.webscare.urducanvas.viewmodels.MainViewModel
import com.webscare.urducanvas.viewmodels.SearchScope
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TextAdjustmentsFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentTextAdjustmentsBinding? = null
    private val binding get() = _binding!!

    private var mediator: TabLayoutMediator? = null
    private val tabs = listOf("Styles", "Font", "Appearance", "3D", "Format", "Symbols")
    private lateinit var adapter: TextAdjustmentsPagerAdapter

    /** Drives which header tools apply to the page on screen. */
    private var currentTab: Int = TAB_STYLES

    private val viewModel: CanvasViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextAdjustmentsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = getString(R.string.text_properties)
        binding.viewPager.isSaveEnabled = false
        binding.viewPager.adapter = null

        val isMixedGroup = arguments?.getBoolean("isMixedGroup") ?: false
        val groupId = arguments?.getString("groupId")
        val elementId = arguments?.getString("elementId")
        if (isMixedGroup) {
            binding.groupToggleContainer.visibility = View.VISIBLE
            val toggleAction = {
                val bundle = Bundle().apply {
                    putString("elementId", elementId)
                    putBoolean("isMixedGroup", true)
                    putString("groupId", groupId)
                }
                val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                findNavController().navigate(R.id.adjustmentsParentFragment, bundle, navOptions)
            }
            binding.btnPrevGroupTab.addPressEffect { toggleAction() }
            binding.btnNextGroupTab.addPressEffect { toggleAction() }
        }

        adapter = TextAdjustmentsPagerAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            tabs
        )
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        setupTabLayout()
        setupSearchBar()
        setupCalligraphyButton()
        renderSearchIcon()
        renderCalligraphyButton()

        binding.back.addPressEffect {
            findNavController().navigateUp()
        }

        viewModel.openAppearanceTab.observe(viewLifecycleOwner) { openAppearance ->
            if (!isAdded || _binding == null) return@observe
            if (openAppearance == true) {
                binding.viewPager.post {
                    if (_binding == null) return@post
                    binding.viewPager.setCurrentItem(2, false)
                }
            }
        }

        viewModel.open3DTab.observe(viewLifecycleOwner) { open3d ->
            if (!isAdded || _binding == null) return@observe
            if (open3d == true) {
                binding.viewPager.post {
                    if (_binding == null) return@post
                    binding.viewPager.setCurrentItem(3, false)
                }
            }
        }
    }

    // ── TabLayout ─────────────────────────────────────────────────────────────

    /**
     * Authoritative page tracking. The TabLayout callback only fires for user
     * taps, so it would miss the restore in [setupTabLayout] and the programmatic
     * jumps into Appearance / 3D — leaving the header tools on the wrong tab.
     */
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentTab = position
            renderSearchIcon()
            renderCalligraphyButton()
        }
    }

    private fun setupTabLayout() {
        mediator?.detach()
        mediator = binding.tabLayout.setupPanelTabs(binding.viewPager, tabs) { position ->
            // Each searchable tab owns its query now, so leaving one no longer has to throw
            // the filter away to keep it off the next tab. Switching back shows the term you
            // left there, and the icon says so.
            viewModel.lastTextAdjustmentsTab = position
        }

        // Reopen on the tab the user left from — coming back to Symbols after
        // closing the panel is the common case while composing calligraphy.
        val restore = viewModel.lastTextAdjustmentsTab
        if (restore in tabs.indices && restore != 0) {
            binding.viewPager.post {
                if (_binding == null) return@post
                binding.viewPager.setCurrentItem(restore, false)
            }
        }
    }

    /**
     * The query the tab on screen searches, or null if that tab has no list to filter.
     *
     * This is the whole of the scoping fix as the user meets it: the icon, the dialog it
     * opens and the term it clears all resolve through here, so they always act on the tab
     * in front of them and never on a sibling.
     */
    private fun scopeForCurrentTab(): String? = when (currentTab) {
        TAB_STYLES  -> SearchScope.TEXT_STYLES
        TAB_FONT    -> SearchScope.TEXT_FONT
        TAB_3D      -> SearchScope.TEXT_3D
        TAB_SYMBOLS -> SearchScope.TEXT_SYMBOLS
        else        -> null
    }

    private fun setupSearchBar() {
        binding.searchIcon.addPressEffect {
            val scope = scopeForCurrentTab() ?: return@addPressEffect
            if (mainViewModel.queryFor(scope).value.isNotEmpty()) {
                // The icon is a cross at this point — it undoes the filter
                // rather than reopening the dialog on top of it.
                mainViewModel.setQuery(scope, "")
            } else {
                com.webscare.urducanvas.ui.editor.panels.adjustments.PanelSearchDialogFragment
                    .newInstance("text_adjustments", scope)
                    .show(childFragmentManager, "panel_search_dialog")
            }
        }

        // One collector per searchable scope. The icon shows whichever one the tab on
        // screen belongs to, so a term typed on Font cannot light the icon up on Styles.
        SearchScope.TEXT_ADJUSTMENTS_TABS.forEach { scope ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    mainViewModel.queryFor(scope).collect {
                        if (scopeForCurrentTab() == scope) renderSearchIcon()
                    }
                }
            }
        }
    }

    /**
     * Search is meaningful on the four tabs that show a list of named things — Styles and
     * Font, plus 3D (its Presets page) and Symbols, whose glyphs all carry a name the tile
     * does not print. Appearance and Format are colour swatches, sliders and statically
     * inflated cards with no user-facing text to match, so the icon stays hidden there
     * rather than offering a search that could only ever return nothing.
     *
     * The icon doubles as the "a filter is on" indicator for the tab that is open, and it
     * reads that tab's own query — which is what makes the state follow the tabs.
     */
    private fun renderSearchIcon() {
        val b = _binding ?: return
        val scope = scopeForCurrentTab()
        b.searchIcon.isVisible = scope != null
        if (scope == null) return

        val hasQuery = mainViewModel.queryFor(scope).value.isNotEmpty()
        b.searchIcon.setImageResource(if (hasQuery) R.drawable.ic_close else R.drawable.ic_search)
        b.searchIcon.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                if (hasQuery) R.color.appColor else R.color.gray
            )
        )
        b.searchIcon.contentDescription = getString(
            if (hasQuery) R.string.search_clear else R.string.search_here
        )
    }

    // ── Calligraphy break / rejoin ────────────────────────────────────────────

    private fun setupCalligraphyButton() {
        binding.btnCalligraphyBreakdown.addPressEffect {
            val element = viewModel.selectedTextElement() ?: return@addPressEffect

            // Once the text is broken apart the button IS the way back — no need
            // to reopen the sheet just to reach Rejoin.
            if (element.calligraphyData?.tokens?.isNotEmpty() == true) {
                viewModel.collapseSelectedCalligraphyToText()
                viewModel.exitCalligraphyMode(saveAsComposition = false)
                return@addPressEffect
            }

            // Only the sheet being opened is reported here. Which of its three buttons was
            // pressed already reaches GA4 as feature_completed with an "expand_words",
            // "expand_characters" or "rejoin" detail from the view model, so logging the
            // buttons too would count every calligraphy breakdown twice. What was missing is
            // the denominator: how often the sheet is opened and abandoned.
            viewModel.logToolAction("text", "calligraphy_breakdown", "sheet_opened")
            val sheet = CalligraphyBreakdownBottomSheet.newInstance(isAlreadyExpanded = false)
            sheet.onBreakWords = {
                viewModel.expandSelectedTextToCalligraphy(ExpansionDepth.WORDS)
            }
            sheet.onBreakCharacters = {
                viewModel.expandSelectedTextToCalligraphy(ExpansionDepth.CHARACTERS)
            }
            sheet.onCollapse = {
                viewModel.collapseSelectedCalligraphyToText()
            }
            sheet.show(childFragmentManager, "calligraphy_breakdown_sheet")
        }

        viewModel.canvasElements.observe(viewLifecycleOwner) { renderCalligraphyButton() }
        viewModel.isCalligraphyEditMode.observe(viewLifecycleOwner) { renderCalligraphyButton() }
    }

    private fun renderCalligraphyButton() {
        val b = _binding ?: return
        val isCalligraphyOn = viewModel.isCalligraphyEditMode.value == true ||
            (viewModel.selectedTextElement()?.calligraphyData?.tokens?.isNotEmpty() == true)
        val shouldShow = (currentTab == TAB_SYMBOLS) || isCalligraphyOn
        b.btnCalligraphyBreakdown.isVisible = shouldShow
        if (!shouldShow) return

        // The same button flips role: it breaks the text apart, then puts it back.
        val expanded = viewModel.selectedTextElement()
            ?.calligraphyData?.tokens?.isNotEmpty() == true
        b.btnCalligraphyBreakdown.setImageResource(
            if (expanded) R.drawable.ic_group else R.drawable.ic_magic_wand
        )
        b.btnCalligraphyBreakdown.setColorFilter(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
        )
        b.btnCalligraphyBreakdown.contentDescription = getString(
            if (expanded) R.string.calligraphy_rejoin else R.string.symbols_calligraphy
        )
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        _binding?.viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding?.viewPager?.adapter = null
        // Every tab's filter goes when the panel does. See MainViewModel.clearQueries for
        // why the line is drawn here and not at the tab switch.
        mainViewModel.clearQueries(SearchScope.TEXT_ADJUSTMENTS_TABS)
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAB_STYLES = 0
        const val TAB_FONT = 1
        const val TAB_3D = 3
        const val TAB_SYMBOLS = 5
    }
}