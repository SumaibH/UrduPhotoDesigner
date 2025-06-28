package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.PickerTarget
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.databinding.FragmentGradientColorListBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradientColorListFragment : Fragment() {
    private var _binding: FragmentGradientColorListBinding? = null
    private val binding get() = _binding!!
    private lateinit var colorsAdapter: ColorsAdapter
    private val viewModel: GradientViewModel by activityViewModels()
    private val canvasViewModel: CanvasViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradientColorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObserver()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            viewModel.updateSelectedStopColor(color.colorCode.toColorInt())
        }, {
            viewModel.updateSelectedStopColor(android.R.color.transparent)
        }, {
            canvasViewModel.startPicking(PickerTarget.COLOR_PICKER_GRADIENT)
            childFragmentManager
                .beginTransaction()
                .replace(R.id.gradientColorFragment, ColorPickerFragment())
                .addToBackStack(null)
                .commit()
        }, {
            canvasViewModel.startPicking(PickerTarget.COLOR_PICKER_GRADIENT)
            //need to update gradient viewmodel here for new color

            //from editor to list, list to picker, update viewmodels as well
        })
        binding.colors.apply {
            adapter = colorsAdapter
        }
    }

    private fun initObserver() {
        canvasViewModel.gradientStopColor.observe(viewLifecycleOwner) { color ->
            viewModel.updateSelectedStopColor(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}