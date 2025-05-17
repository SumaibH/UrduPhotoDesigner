package com.example.urduphotodesigner.ui.editor.panels.background.gradients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.common.Constants
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentColorsListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradientsListFragment : Fragment() {
    private var _binding: FragmentColorsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var gradientsAdapter: GradientsAdapter
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
    }

    private fun setupRecyclerView() {
        gradientsAdapter = GradientsAdapter(Constants.gradientList){ selectedGradient ->
            viewModel.setCanvasBackgroundImage(selectedGradient)
        }
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