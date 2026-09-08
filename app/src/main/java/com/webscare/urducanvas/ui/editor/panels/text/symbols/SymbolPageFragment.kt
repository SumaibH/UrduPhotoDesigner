package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.webscare.urducanvas.databinding.FragmentSymbolPageBinding

class SymbolPageFragment : Fragment() {

    private var _binding: FragmentSymbolPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SymbolGridAdapter
    private var category: SymbolCategory = SymbolCategory.UPPER

    companion object {
        private const val ARG_CATEGORY = "arg_category"

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

        binding.rvSymbols.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvSymbols.adapter = adapter

        val symbols = SymbolsRepository.getSymbolsForCategory(category)
        adapter.submitList(symbols)
    }

    override fun onDestroyView() {
        binding.rvSymbols.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
