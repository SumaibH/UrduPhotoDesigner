package com.example.urduphotodesigner.ui.editor.panels.text.colors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentColorsListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorsListFragment : Fragment() {
    private var _binding: FragmentColorsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorsAdapter: ColorsAdapter
    private val viewModel: CanvasViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObservers()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList) { color ->
            viewModel.setTextColor(color.colorCode.toColorInt())
            // No need to manually update adapter.selectedColor or call notifyDataSetChanged() here.
            // The observer in initObservers() will handle updating the adapter's state
            // and triggering the precise UI update.
        }
        binding.colors.apply {
            adapter = colorsAdapter
        }
    }

    private fun initObservers() {
        viewModel.currentTextColor.observe(viewLifecycleOwner) { color ->
            // When the ViewModel's current text color changes (e.g., due to canvas selection),
            // update the adapter's selectedColor. This will trigger the efficient
            // notifyItemChanged calls within the adapter's setter.
            colorsAdapter.selectedColor = color!!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): ColorsListFragment {
            return ColorsListFragment()
        }
    }
}