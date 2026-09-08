package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.enums.ElementType
import com.webscare.urducanvas.common.canvas.model.CanvasElement
import com.webscare.urducanvas.common.canvas.model.ExpansionDepth
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.FragmentTextSymbolsBinding
import com.webscare.urducanvas.ui.editor.views.RailCategoryItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TextSymbolsFragment : Fragment() {

    private var _binding: FragmentTextSymbolsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var pagerAdapter: TextSymbolsPagerAdapter

    // No iconRes on purpose: the rail then renders the label's first letter as a
    // monogram tile when collapsed, exactly like the Text Styles rail. There is
    // no icon in the set that reads as "diacritic above/below", and the nearest
    // ones (alignment bars, chevrons) looked like alignment or expand controls.
    private val categories by lazy {
        listOf(
            RailCategoryItem("upper", getString(R.string.symbols_above)),
            RailCategoryItem("lower", getString(R.string.symbols_below)),
            RailCategoryItem("side", getString(R.string.symbols_marks)),
            RailCategoryItem("dots", getString(R.string.symbols_ornaments))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextSymbolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRailAndPager()
        setupActions()
        observeCanvasElements()
    }

    override fun onResume() {
        super.onResume()
        // The character strip is rendered by the editor header, not by us.
        viewModel.setCharacterBarVisible(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.setCharacterBarVisible(false)
    }

    private fun setupRailAndPager() {
        binding.collapsibleRail.bindPanelId("text_symbols")
        binding.collapsibleRail.setCategories(categories)

        binding.collapsibleRail.onCategorySelectedListener = { catItem ->
            val index = categories.indexOfFirst { it.id == catItem.id }
            if (index >= 0) {
                binding.viewPager.setCurrentItem(index, true)
            }
        }

        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.offscreenPageLimit = 1

        pagerAdapter = TextSymbolsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position in categories.indices) {
                    binding.collapsibleRail.setSelectedCategory(categories[position].id)
                }
            }
        })
    }

    private fun setupActions() {
        binding.btnDeleteLastDiacritic.addPressEffect {
            viewModel.removeLastDiacriticFromSelectedChar(viewModel.resolvedCharIndex())
        }

        binding.btnClearAllDiacritics.addPressEffect {
            viewModel.clearAllDiacriticsFromSelectedChar(viewModel.resolvedCharIndex())
        }

        binding.btnCalligraphyBreakdown.addPressEffect {
            val element = getSelectedTextElement() ?: return@addPressEffect

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
    }

    private fun observeCanvasElements() {
        viewModel.canvasElements.observe(viewLifecycleOwner) { refreshActionRow() }
        viewModel.isCalligraphyEditMode.observe(viewLifecycleOwner) { refreshActionRow() }
    }

    /**
     * The per-letter actions only mean something once the text has been broken
     * apart, so they stay hidden until then. Calligraphy itself always shows —
     * it is what performs the break.
     */
    private fun refreshActionRow() {
        val b = _binding ?: return
        val element = getSelectedTextElement()
        val expanded = element?.calligraphyData?.tokens?.isNotEmpty() == true

        b.tvEmptyState.visibility = if (element == null) View.VISIBLE else View.GONE
        b.btnDeleteLastDiacritic.visibility = if (expanded) View.VISIBLE else View.GONE
        b.btnClearAllDiacritics.visibility = if (expanded) View.VISIBLE else View.GONE

        // The same button flips role: it breaks the text apart, then puts it back.
        b.tvCalligraphyLabel.setText(
            if (expanded) R.string.calligraphy_rejoin else R.string.symbols_calligraphy
        )
        b.ivCalligraphyIcon.setImageResource(
            if (expanded) R.drawable.ic_group else R.drawable.ic_magic_wand
        )
    }

    private fun getSelectedTextElement(): CanvasElement? {
        return viewModel.canvasElements.value?.firstOrNull {
            it.isSelected && it.type == ElementType.TEXT
        }
    }

    /** Called by [SymbolPageFragment] when a symbol tile is tapped. */
    fun handleSymbolTapped(symbol: SymbolItem) {
        val element = getSelectedTextElement() ?: return
        val index = viewModel.resolvedCharIndex()

        if (symbol.isDiacritic) {
            viewModel.appendDiacriticToSelectedChar(index, symbol.glyph)
        } else {
            if (element.calligraphyData != null) {
                viewModel.addFloatingCalligraphyAccent(symbol.glyph)
            } else {
                viewModel.appendDiacriticToSelectedChar(index, symbol.glyph)
            }
        }
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): TextSymbolsFragment {
            return TextSymbolsFragment()
        }
    }
}
