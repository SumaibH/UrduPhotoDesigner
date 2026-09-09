package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.enums.ElementType
import com.webscare.urducanvas.databinding.FragmentSymbolPageBinding

class SymbolPageFragment : Fragment() {

    private var _binding: FragmentSymbolPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()

    private lateinit var adapter: SymbolGridAdapter
    private var category: SymbolCategory = SymbolCategory.UPPER

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        /** Tile height in dp, kept in sync with item_symbol_card.xml (52dp card + 3dp margins each side). */
        private const val ROW_HEIGHT_DP = 58f

        /** Never stack more than this many rows, however tall the panel gets. */
        private const val MAX_ROWS = 3

        fun newInstance(category: SymbolCategory): SymbolPageFragment {
            return SymbolPageFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getSerializable(ARG_CATEGORY) as? SymbolCategory ?: SymbolCategory.UPPER
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSymbolPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SymbolGridAdapter { symbol ->
            (parentFragment as? TextSymbolsFragment)?.handleSymbolTapped(symbol)
        }

        // Rows that scroll sideways, matching every other editor panel. The row
        // count is derived from the height actually available so the grid neither
        // clips a half row in the resting panel nor leaves a gaping hole when the
        // sheet is expanded.
        binding.rvSymbols.layoutManager = GridLayoutManager(
            requireContext(), 1, RecyclerView.HORIZONTAL, false
        )
        binding.rvSymbols.adapter = adapter
        // The page root fills the pager, so it reports the height actually on offer.
        binding.root.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) updateRowCount()
        }
        binding.root.post { updateRowCount() }

        adapter.submitList(SymbolsRepository.getSymbolsForCategory(category))

        // Previews follow the font of whatever text layer is selected.
        viewModel.canvasElements.observe(viewLifecycleOwner) { elements ->
            if (_binding == null) return@observe
            val typeface = elements
                ?.firstOrNull { it.isSelected && it.type == ElementType.TEXT }
                ?.typefaceOrNull
            adapter.setPreviewTypeface(typeface)
        }
    }

    /**
     * Fits as many tile rows as the panel currently allows, at least one.
     *
     * The grid is wrap_content, so it measures to exactly rows x tile height —
     * a fill-height grid would instead divide the whole panel by the row count
     * and strand the tiles in huge vertical gaps once the sheet is expanded.
     */
    private fun updateRowCount() {
        val b = _binding ?: return
        val available = b.root.height - b.rvSymbols.paddingTop - b.rvSymbols.paddingBottom
        if (available <= 0) return

        val rowPx = ROW_HEIGHT_DP * resources.displayMetrics.density
        val rows = (available / rowPx).toInt().coerceIn(1, MAX_ROWS)

        val lm = b.rvSymbols.layoutManager as? GridLayoutManager ?: return
        if (lm.spanCount == rows) return

        // Posted, not applied inline: this runs from a layout-change callback,
        // and a requestLayout() raised mid-traversal is dropped. The grid then
        // kept the row count it was measured with — three rows in a box that
        // only had room for two, so the bottom row was sliced off.
        b.rvSymbols.post {
            val rv = _binding?.rvSymbols ?: return@post
            (rv.layoutManager as? GridLayoutManager)?.let {
                if (it.spanCount != rows) it.spanCount = rows
            }
        }
    }

    override fun onDestroyView() {
        binding.rvSymbols.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
