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
            RailCategoryItem("quranic", getString(R.string.symbols_quranic)),
            RailCategoryItem("honorifics", getString(R.string.symbols_honorifics)),
            RailCategoryItem("ornaments", getString(R.string.symbols_ornaments))
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
        observeCanvasElements()
    }

    override fun onResume() {
        super.onResume()
        // The character strip floats over the canvas, not in this panel.
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

    private fun observeCanvasElements() {
        viewModel.canvasElements.observe(viewLifecycleOwner) { refreshActionRow() }
        viewModel.isCalligraphyEditMode.observe(viewLifecycleOwner) { refreshActionRow() }
    }

    private fun refreshActionRow() {
        val b = _binding ?: return
        val element = getSelectedTextElement()
        b.tvEmptyState.visibility = if (element == null) View.VISIBLE else View.GONE
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
            return
        }

        // Kashida is not an ornament. It stretches the join between two letters, so it
        // belongs to the token as an elongation rather than as a mark dropped on top of
        // the composition — which is what treating it as a floating accent produced: a
        // tatweel sitting loose on the canvas, unattached to any letter.
        if (symbol.id == SYMBOL_KASHIDA && element.calligraphyData != null) {
            viewModel.adjustKashidaOnSelectedTokens(+1)
            return
        }

        if (element.calligraphyData != null) {
            viewModel.addFloatingCalligraphyAccent(symbol.glyph)
        } else {
            viewModel.appendDiacriticToSelectedChar(index, symbol.glyph)
        }
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Matches the SymbolItem id in SymbolsRepository. */
        private const val SYMBOL_KASHIDA = "kashida"

        fun newInstance(): TextSymbolsFragment {
            return TextSymbolsFragment()
        }
    }
}
