package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.GradientType
import com.example.urduphotodesigner.databinding.FragmentGradientEditorBinding
import com.example.urduphotodesigner.databinding.FragmentGradientSettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradientSettingFragment : Fragment() {
    private var _binding: FragmentGradientSettingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradientSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        initObservers()
    }

    private fun initObservers() {
        viewModel.gradient.observe(viewLifecycleOwner) { gradient ->
            val safeScale = gradient.scale.coerceIn(0.1f, 2f)
            binding.scale.progress = (safeScale * 100).toInt()
            binding.scaleSize.text = String.format("%.2f", safeScale)

            val safeAngle = gradient.angle.mod(360f).toInt()
            binding.angle.progress = safeAngle
            binding.angleSize.text = "$safeAngle\u00B0"

            when (gradient.type) {
                GradientType.LINEAR -> { updateButtonTints(binding.linear)}
                GradientType.RADIAL -> { updateButtonTints(binding.radial)}
                else -> {updateButtonTints(binding.sweep)}
            }
        }
    }

    private fun updateButtonTints(selected: View) {
        // Colors
        val contrastColor = ContextCompat.getColor(requireContext(), R.color.contrast)
        val defaultColor  = ContextCompat.getColor(requireContext(), R.color.white)

        // List your three buttons here
        val buttons = listOf(binding.linear, binding.radial, binding.sweep)

        buttons.forEach { btn ->
            val tint = if (btn == selected) defaultColor else contrastColor
            btn.backgroundTintList = ColorStateList.valueOf(tint)
        }
    }

    private fun setEvents() {
        binding.back.setOnClickListener {
            viewModel.setPagingLocked(false)
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        binding.linear.setOnClickListener {
            viewModel.setType(GradientType.LINEAR)
        }

        binding.radial.setOnClickListener {
            viewModel.setType(GradientType.RADIAL)
        }

        binding.sweep.setOnClickListener {
            viewModel.setType(GradientType.SWEEP)
        }

        binding.scale.apply {
            min = 10
            max = 200
            val initScale = (viewModel.gradient.value?.scale ?: 1f)
                .coerceIn(0.1f, 2f)
            progress = (initScale * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val scale = (progress.coerceIn(10, 200) / 100f)
                        viewModel.setScale(scale)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.angle.apply {
            min = 0
            max = 360
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        viewModel.setAngle(progress.toFloat())
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}