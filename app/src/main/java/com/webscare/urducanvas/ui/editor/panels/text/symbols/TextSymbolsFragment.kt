package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.enums.ElementType
import com.webscare.urducanvas.common.canvas.model.CanvasElement
import com.webscare.urducanvas.common.canvas.model.ExpansionDepth
import com.webscare.urducanvas.common.utils.CalligraphyShapingHelper
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.FragmentTextSymbolsBinding
import com.webscare.urducanvas.ui.editor.views.RailCategoryItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TextSymbolsFragment : Fragment() {

    private var _binding: FragmentTextSymbolsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var charAdapter: SymbolCharAdapter
    private lateinit var pagerAdapter: TextSymbolsPagerAdapter

    private var selectedCharIndex = 0

    private val categories = listOf(
        RailCategoryItem("upper", "اوپری اعراب", R.drawable.ic_up),
        RailCategoryItem("lower", "نچلے اعراب", R.drawable.ic_down),
        RailCategoryItem("side", "رموز و علامات", R.drawable.ic_kasheeda),
        RailCategoryItem("dots", "نقطے و پھول", R.drawable.ic_shapes)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextSymbolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRailAndPager()
        setupCharStrip()
        setupActions()
        observeCanvasElements()
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

    private fun setupCharStrip() {
        charAdapter = SymbolCharAdapter { index ->
            selectedCharIndex = index
            val element = getSelectedTextElement()
            element?.calligraphyData?.let { cData ->
                if (index in cData.tokens.indices) {
                    cData.activeTokenId = cData.tokens[index].id
                }
            }
            refreshCharacterStrip(element)
        }
        binding.rvCharacters.adapter = charAdapter
    }

    private fun setupActions() {
        binding.btnToggleDotless.addPressEffect {
            viewModel.toggleDotlessOnSelectedChar(selectedCharIndex)
        }

        binding.btnDeleteLastDiacritic.addPressEffect {
            viewModel.removeLastDiacriticFromSelectedChar(selectedCharIndex)
        }

        binding.btnClearAllDiacritics.addPressEffect {
            viewModel.clearAllDiacriticsFromSelectedChar(selectedCharIndex)
        }

        binding.btnCalligraphyBreakdown.addPressEffect {
            val element = getSelectedTextElement() ?: return@addPressEffect
            val isAlready = element.calligraphyData != null
            val sheet = CalligraphyBreakdownBottomSheet.newInstance(isAlreadyExpanded = isAlready)
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
        viewModel.canvasElements.observe(viewLifecycleOwner) { _ ->
            val element = getSelectedTextElement()
            refreshCharacterStrip(element)
        }
    }

    private fun getSelectedTextElement(): CanvasElement? {
        return viewModel.canvasElements.value?.firstOrNull {
            it.isSelected && it.type == ElementType.TEXT
        }
    }

    private fun refreshCharacterStrip(element: CanvasElement?) {
        if (_binding == null) return
        if (element == null) {
            charAdapter.submitList(emptyList())
            binding.tvPreviewChar.text = "-"
            return
        }

        val cData = element.calligraphyData
        if (cData != null && cData.tokens.isNotEmpty()) {
            // Expanded Calligraphy Mode: Tokens are items
            binding.btnCalligraphyBreakdown.text = "✨ خطاطی (فعال)"
            val activeToken = cData.getActiveToken() ?: cData.tokens.firstOrNull()
            val activeIdx = if (activeToken != null) cData.tokens.indexOf(activeToken) else 0
            selectedCharIndex = if (activeIdx >= 0) activeIdx else 0

            val chips = cData.tokens.mapIndexed { idx, token ->
                CharChipModel(
                    index = idx,
                    displayText = token.getFullDisplayText(),
                    rawChar = token.rawText,
                    isSelected = (idx == selectedCharIndex)
                )
            }
            charAdapter.submitList(chips)
            binding.tvPreviewChar.text = activeToken?.getFullDisplayText() ?: "-"
        } else {
            // Standard Text Mode: Split into letters with attached diacritics
            binding.btnCalligraphyBreakdown.text = "✨ خطاطی"
            val text = element.text
            val letterClusters = buildLetterClusters(text)

            if (selectedCharIndex >= letterClusters.size) {
                selectedCharIndex = (letterClusters.size - 1).coerceAtLeast(0)
            }

            val chips = letterClusters.mapIndexed { idx, cluster ->
                CharChipModel(
                    index = idx,
                    displayText = cluster.displayText,
                    rawChar = cluster.rawChar,
                    isSelected = (idx == selectedCharIndex)
                )
            }
            charAdapter.submitList(chips)

            val activeCluster = letterClusters.getOrNull(selectedCharIndex)
            binding.tvPreviewChar.text = activeCluster?.displayText ?: "-"
        }
    }

    data class LetterCluster(
        val rawChar: String,
        val displayText: String,
        val charIndexInString: Int
    )

    private fun buildLetterClusters(text: String): List<LetterCluster> {
        val result = mutableListOf<LetterCluster>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch.isWhitespace()) {
                i++
                continue
            }
            val startIdx = i
            val sb = StringBuilder().append(ch)
            i++
            while (i < text.length && CalligraphyShapingHelper.isDiacritic(text[i])) {
                sb.append(text[i])
                i++
            }
            result.add(
                LetterCluster(
                    rawChar = ch.toString(),
                    displayText = sb.toString(),
                    charIndexInString = startIdx
                )
            )
        }
        return result
    }

    /**
     * Called by [SymbolPageFragment] when any symbol card is tapped.
     */
    fun handleSymbolTapped(symbol: SymbolItem) {
        val element = getSelectedTextElement() ?: return

        if (symbol.isDiacritic) {
            viewModel.appendDiacriticToSelectedChar(selectedCharIndex, symbol.glyph)
        } else {
            val cData = element.calligraphyData
            if (cData != null) {
                viewModel.addFloatingCalligraphyAccent(symbol.glyph)
            } else {
                viewModel.appendDiacriticToSelectedChar(selectedCharIndex, symbol.glyph)
            }
        }
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        binding.rvCharacters.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): TextSymbolsFragment {
            return TextSymbolsFragment()
        }
    }
}
