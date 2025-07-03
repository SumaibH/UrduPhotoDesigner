package com.example.urduphotodesigner.ui.editor.panels.background.gradients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentFillStrokeBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient.GradientEditorFragment
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GradientsListFragment : Fragment() {
    private var _binding: FragmentFillStrokeBinding? = null
    private val binding get() = _binding!!

    private lateinit var gradientsAdapter: GradientsAdapter
    private val viewModel: CanvasViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFillStrokeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObservers()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            mainViewModel.gradients.observe(viewLifecycleOwner) { gradients ->
                gradientsAdapter.updateList(gradients)
            }
        }
    }

    private fun setupRecyclerView() {
        gradientsAdapter = GradientsAdapter(
            gradientList = emptyList(),
            onGradientSelected = { _, gradient ->
                viewModel.setCanvasGradient(gradient)
            },
            onNoneSelected = {
                viewModel.removeCanvasBackgroundImage()
            },
            onGradientEditSelected = { _, item ->
                viewModel.setGradient(item)
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fillStroke, GradientEditorFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean("IS_EDIT", true)
                        }})
                    .addToBackStack(null)
                    .commit()
            },
            onGradientPickerClicked = {
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fillStroke, GradientEditorFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean("IS_EDIT", false)
                        }})
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.colors.apply {
            adapter = gradientsAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): GradientsListFragment {
            return GradientsListFragment()
        }
    }
}