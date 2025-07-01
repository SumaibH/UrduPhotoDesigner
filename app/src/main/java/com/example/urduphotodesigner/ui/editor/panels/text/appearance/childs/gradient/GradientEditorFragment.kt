package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.GradientType
import com.example.urduphotodesigner.databinding.FragmentGradientEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GradientEditorFragment : Fragment() {
    private var _binding: FragmentGradientEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()

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
        initObservers()
    }

    private fun initObservers() {
        viewModel.gradient.observe(viewLifecycleOwner) { gradient ->
            binding.gradientBar.gradientItem = gradient
            val drawable = gradient.createGradientPreviewDrawable(
                gradient = gradient,
                width = binding.preview.width.takeIf { it > 0 } ?: 600,
                height = binding.preview.height.takeIf { it > 0 } ?: 200
            )
            binding.preview.setImageDrawable(drawable)
            binding.gradientBar.invalidate()
        }
    }

    private fun setEvents() {
        binding.back.setOnClickListener {
            viewModel.setPagingLocked(false)
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
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
            onStopRemoved = {idx ->
                viewModel.removeStop(idx)
            }
        }

        binding.swap.setOnClickListener { viewModel.swapGradientStops() }

        binding.settings.setOnClickListener {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.gradientEditor, GradientSettingFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}