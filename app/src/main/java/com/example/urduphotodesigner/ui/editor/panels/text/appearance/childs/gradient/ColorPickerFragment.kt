package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentColorPickerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorPickerFragment : Fragment() {
    private var _binding: FragmentColorPickerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()
    private var currentHue = 0f

    // rainbow stops for hue bar
    private val rainbow = intArrayOf(
        Color.RED, Color.YELLOW, Color.GREEN,
        Color.CYAN, Color.BLUE, Color.MAGENTA,
        Color.RED
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.back.setOnClickListener { parentFragment?.childFragmentManager?.popBackStack() }
        setupHueBar()
        setupAlphaBar()
        binding.seekbarHue.onProgressChanged?.invoke(binding.seekbarHue.progress.toInt())
    }

    private fun setupHueBar() {
        binding.seekbarHue.apply {
            max = 360
            setGradient(rainbow)
            progress = 0f
            onProgressChanged = { hueDeg ->
                currentHue = hueDeg.toFloat()

                val opaque = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f))
                val rawTransparent = opaque and 0x00FFFFFF
                val bakedTransparent = bakeAlpha(rawTransparent)
                binding.seekbarAlpha.setGradient(intArrayOf(bakedTransparent, opaque))

                binding.seekbarAlpha.progress = binding.seekbarAlpha.progress

                val alphaInt = binding.seekbarAlpha.progress.toInt()
                val composite = bakeAlpha((alphaInt shl 24) or (opaque and 0x00FFFFFF))
                viewModel.finishPicking(composite)
            }
        }
    }

    private fun setupAlphaBar() {
        binding.seekbarAlpha.apply {
            max = 255
            val opaque = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f))
            val rawTransparent = opaque and 0x00FFFFFF
            val bakedTransparent = bakeAlpha(rawTransparent)

            setGradient(intArrayOf(bakedTransparent, opaque))
            progress = max.toFloat()
            onProgressChanged = { alphaVal ->
                val hsvRgb = Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)) and 0x00FFFFFF
                val composite = bakeAlpha((alphaVal shl 24) or hsvRgb)
                viewModel.finishPicking(composite)
            }
        }
    }

    /** Composite srcColor onto white background. */
    private fun bakeAlpha(srcColor: Int, bgColor: Int = Color.WHITE): Int {
        val a = Color.alpha(srcColor)
        val r = Color.red(srcColor)
        val g = Color.green(srcColor)
        val b = Color.blue(srcColor)
        val br = Color.red(bgColor)
        val bg = Color.green(bgColor)
        val bb = Color.blue(bgColor)
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