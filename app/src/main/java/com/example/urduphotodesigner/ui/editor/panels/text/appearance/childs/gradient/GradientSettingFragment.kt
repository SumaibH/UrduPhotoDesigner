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

    private var pendingScale: Float    = 1f
    private var pendingAngle: Float    = 0f

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

    private fun syncUiToPending() {
        // update the SeekBars/text fields
        binding.scale.progress     = (pendingScale.coerceIn(0.1f,2f) * 100).toInt()
        binding.scaleSize.text     = String.format("%.2f", pendingScale)
        binding.angle.progress     = pendingAngle.mod(360f).toInt()
        binding.angleSize.text     = "${pendingAngle.mod(360f).toInt()}°"
    }

    private fun initObservers() {
        viewModel.gradient.value?.let {
            pendingScale = it.scale
            pendingAngle = it.angle
            syncUiToPending()
        }

        viewModel.gradient.observe(viewLifecycleOwner) { gradient ->
            val safeScale = gradient.scale.coerceIn(0.1f, 2f)
            binding.scale.progress = (safeScale * 100).toInt()
            binding.scaleSize.text = String.format("%.2f", safeScale)

            val safeAngle = gradient.angle.mod(360f).toInt()
            binding.angle.progress = safeAngle
            binding.angleSize.text = "$safeAngle\u00B0"
        }
    }

    private fun setEvents() {
        binding.back.setOnClickListener {
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        binding.done.setOnClickListener {
            viewModel.setScale(pendingScale)
            viewModel.setAngle(pendingAngle)
            parentFragment?.childFragmentManager?.popBackStack()
        }

        binding.scale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingScale = progress / 100f
                    binding.scaleSize.text = String.format("%.2f", pendingScale)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.angle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingAngle = progress.toFloat()
                    binding.angleSize.text = "${progress}°"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}