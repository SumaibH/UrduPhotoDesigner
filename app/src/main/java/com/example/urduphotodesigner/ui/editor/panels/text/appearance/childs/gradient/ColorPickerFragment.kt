package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

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
    private val trackHeightDp = 12f
    private val trackHeightPx get() = (trackHeightDp * dp).toInt()
    private val cornerRadiusPx get() = trackHeightPx / 2f

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
        setupEvents()
    }

    private fun setupEvents() {
        binding.back.setOnClickListener {
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }
    }

    private fun setupSeekbars() {
        updateAlphaBar(0f)

        val thumbOffset = (12f * dp).toInt()
        binding.seekbarHue.thumbOffset = thumbOffset
        binding.seekbarAlpha.thumbOffset = thumbOffset

        binding.seekbarHue.apply {
            max = 360
            progress = 0
            progressDrawable = hueGradient.apply {
                // 2) pill shape at half-height
                cornerRadius = cornerRadiusPx
                setSize(0, trackHeightPx)
            }

            thumb = ContextCompat.getDrawable(requireContext(), R.drawable.seekbar_thumb)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, prog: Int, fromUser: Boolean) {
                    currentHue = prog.toFloat()
                    // Wait until alphaBar has measured
                    binding.seekbarAlpha.post {
                        updateAlphaBar(currentHue)
                        binding.seekbarAlpha.progressDrawable = alphaDrawable
                    }

                    val hsvRgb = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)) and 0x00FFFFFF
                    val alpha = binding.seekbarAlpha.progress
                    val baked = bakeAlpha((alpha shl 24) or hsvRgb)
                    viewModel.finishPicking(baked)
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.seekbarAlpha.post {
            val initialHue = binding.seekbarHue.progress.toFloat()
            updateAlphaBar(initialHue)
            binding.seekbarAlpha.progressDrawable = alphaDrawable
        }

        binding.seekbarAlpha.apply {
            max = 255
            progress = 255
            progressDrawable = alphaDrawable
            thumb = ContextCompat.getDrawable(requireContext(), R.drawable.seekbar_thumb)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, prog: Int, fromUser: Boolean) {
                    val hsvRgb = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)) and 0x00FFFFFF
                    val colorWithAlpha = (prog shl 24) or hsvRgb
                    val solidColor = bakeAlpha(colorWithAlpha)
                    viewModel.finishPicking(solidColor)
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
            cornerRadius = cornerRadiusPx
            setSize(0, trackHeightPx)
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
            setSize(0, trackHeightPx)
            cornerRadius = cornerRadiusPx
        }
        // and then later in your post:
        binding.seekbarAlpha.progressDrawable = alphaDrawable
    }

    fun bakeAlpha(srcColor: Int, bgColor: Int = Color.WHITE): Int {
        val a = Color.alpha(srcColor)
        val r = Color.red(srcColor)
        val g = Color.green(srcColor)
        val b = Color.blue(srcColor)

        val br = Color.red(bgColor)
        val bg = Color.green(bgColor)
        val bb = Color.blue(bgColor)

        // composite formula: out = src * α + bg * (1 – α)
        val outR = (r * a + br * (255 - a)) / 255
        val outG = (g * a + bg * (255 - a)) / 255
        val outB = (b * a + bb * (255 - a)) / 255

        return Color.rgb(outR, outG, outB)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPicking()
        _binding = null
    }
}