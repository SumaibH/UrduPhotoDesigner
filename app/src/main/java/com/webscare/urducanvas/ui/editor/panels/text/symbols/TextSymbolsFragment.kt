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

    /** Latched scrim state. null until the first pass, which lands without a fade. */
    private var isGridInactive: Boolean? = null

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
        // Belt and braces: LiveData only replays to a new observer if it already
        // holds a value. On a cold editor it does not, and without this the grid
        // would sit at full strength with nothing selected — the exact bug.
        refreshInactiveState()
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
        viewModel.canvasElements.observe(viewLifecycleOwner) { refreshInactiveState() }
        viewModel.isCalligraphyEditMode.observe(viewLifecycleOwner) { refreshInactiveState() }
    }

    private fun refreshInactiveState() {
        val b = _binding ?: return
        setGridInactive(getSelectedTextElement() == null, b)
    }

    /**
     * Raises or drops the dim scrim over the symbol grid.
     *
     * [inactive] is recomputed on every canvasElements emission, and the canvas
     * emits on drags, undo, colour changes — dozens of times for one gesture. So
     * the state is latched and a no-op change returns before touching the view:
     * re-running the animator on every emission is what makes an overlay strobe.
     *
     * The very first pass lands without an animation. There is nothing to fade
     * from when the panel is still being laid out, and animating it there is
     * what produces the flash of a fully-lit grid before the scrim arrives.
     */
    private fun setGridInactive(inactive: Boolean, b: FragmentTextSymbolsBinding) {
        if (isGridInactive == inactive) return
        val firstPass = isGridInactive == null
        isGridInactive = inactive

        val overlay = b.inactiveOverlay
        overlay.animate().cancel()

        if (firstPass) {
            overlay.alpha = if (inactive) 1f else 0f
            overlay.visibility = if (inactive) View.VISIBLE else View.GONE
            return
        }

        if (inactive) {
            // Start from wherever the interrupted fade left it, not from 0 — a
            // reset to 0 mid-fade is itself a blink.
            overlay.visibility = View.VISIBLE
            overlay.animate().alpha(1f).setDuration(FADE_MS).start()
        } else {
            overlay.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
                // The scrim is clickable, so leaving it at alpha 0 would keep
                // eating taps on a grid that looks perfectly live.
                _binding?.inactiveOverlay?.visibility = View.GONE
            }.start()
        }
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
        binding.inactiveOverlay.animate().cancel()
        binding.viewPager.adapter = null
        // The fragment outlives its view in a ViewPager2, so the latch has to go
        // with the view. Left set, the next onViewCreated would think the state
        // was unchanged and never raise the scrim on a freshly inflated overlay.
        isGridInactive = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Matches the SymbolItem id in SymbolsRepository. */
        private const val SYMBOL_KASHIDA = "kashida"

        /** Short enough to feel like a state change, long enough not to snap. */
        private const val FADE_MS = 160L

        fun newInstance(): TextSymbolsFragment {
            return TextSymbolsFragment()
        }
    }
}
