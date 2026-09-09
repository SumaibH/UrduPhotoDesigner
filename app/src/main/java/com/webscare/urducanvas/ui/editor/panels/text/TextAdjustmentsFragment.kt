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
            // Styles and Font are the only searchable pages; leaving either one
            // drops the filter so the next visit starts unfiltered.
            if (position != TAB_STYLES && position != TAB_FONT) {
                mainViewModel.setQuery("")
            }
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

    private fun setupSearchBar() {
        binding.searchIcon.addPressEffect {
            if (mainViewModel.searchQuery.value.isNotEmpty()) {
                // The icon is a cross at this point — it undoes the filter
                // rather than reopening the dialog on top of it.
                mainViewModel.setQuery("")
            } else {
                com.webscare.urducanvas.ui.editor.panels.adjustments.PanelSearchDialogFragment
                    .newInstance()
                    .show(childFragmentManager, "panel_search_dialog")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.searchQuery.collect { renderSearchIcon(it) }
            }
        }
    }

    /**
     * Search is only meaningful on the two tabs that show a filterable list, and
     * the icon doubles as the "a filter is on" indicator for whichever of them
     * is open.
     */
    private fun renderSearchIcon(query: String = mainViewModel.searchQuery.value) {
        val b = _binding ?: return
        val searchable = currentTab == TAB_STYLES || currentTab == TAB_FONT
        b.searchIcon.isVisible = searchable
        if (!searchable) return

        val hasQuery = query.isNotEmpty()
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
        mainViewModel.setQuery("")
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAB_STYLES = 0
        const val TAB_FONT = 1
        const val TAB_SYMBOLS = 5
    }
}