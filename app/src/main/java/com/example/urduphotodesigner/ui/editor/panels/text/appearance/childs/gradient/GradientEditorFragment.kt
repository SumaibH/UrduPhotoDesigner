package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentGradientEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradientEditorFragment : Fragment() {
    private var _binding: FragmentGradientEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GradientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradientEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
    }

    private fun setEvents() {
        binding.back.setOnClickListener {
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        viewModel.gradient.observe(viewLifecycleOwner) { gradient ->
            binding.gradientBar.gradientItem = gradient
        }

        // handle callbacks from the view:
        binding.gradientBar.apply {
            onStopAdded = { idx, color, pos ->
                viewModel.addStop(pos, color)
            }
            onStopMoved = { idx, newPos ->
                viewModel.moveStop(idx, newPos)
            }
            onStopSelected = { idx ->
                viewModel.selectStop(idx)
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.gradientEditor, GradientColorListFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}