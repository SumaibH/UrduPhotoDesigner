package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentColorPickerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorPickerFragment : Fragment(R.layout.fragment_color_picker) {
    private var _binding: FragmentColorPickerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()
    private val dp get() = resources.displayMetrics.density
    private var currentHue = 0f
    private lateinit var hueGradient: GradientDrawable
    private lateinit var alphaDrawable: GradientDrawable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColors()
        setupSeekbars()
    }

    private fun setupSeekbars() {
        updateAlphaBar(0f)

        binding.seekbarHue.apply {
            max = 360
            progress = 0
            progressDrawable = hueGradient
            thumb = ContextCompat.getDrawable(requireContext(), R.drawable.seekbar_thumb)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, prog: Int, fromUser: Boolean) {
                    currentHue = prog.toFloat()
                    // Wait until alphaBar has measured
                    binding.seekbarAlpha.post {
                        updateAlphaBar(currentHue)
                        alphaDrawable.setBounds(
                            0, 0,
                            binding.seekbarAlpha.width,
                            binding.seekbarAlpha.height
                        )
                        binding.seekbarAlpha.progressDrawable = alphaDrawable
                    }

                    // Immediately notify color with full opacity
                    val rgb = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f))
                    viewModel.finishPicking((255 shl 24) or (rgb and 0x00FFFFFF))
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.seekbarAlpha.post {
            val initialHue = binding.seekbarAlpha.progress.toFloat()
            updateAlphaBar(initialHue)
            alphaDrawable.setBounds(
                0, 0,
                binding.seekbarAlpha.width,
                binding.seekbarAlpha.height
            )
            binding.seekbarAlpha.progressDrawable = alphaDrawable
        }

        binding.seekbarAlpha.apply {
            max = 255
            progress = 255
            progressDrawable = alphaDrawable
            thumb = ContextCompat.getDrawable(requireContext(), R.drawable.seekbar_thumb)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, prog: Int, fromUser: Boolean) {
                    val rgb = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f))
                    viewModel.finishPicking((prog shl 24) or (rgb and 0x00FFFFFF))
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    private fun setupColors() {
        val rainbow = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN,
            Color.CYAN, Color.BLUE, Color.MAGENTA,
            Color.RED
        )
        hueGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            rainbow
        ).apply {
            cornerRadius = 8f * dp
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
    }

    fun updateAlphaBar(hue: Float) {
        val opaque = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val transparent = opaque and 0x00FFFFFF
        alphaDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(transparent, opaque)
        ).apply {
            cornerRadius = 8f * dp
        }
        // and then later in your post:
        binding.seekbarAlpha.progressDrawable = alphaDrawable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPicking()
        _binding = null
    }
}