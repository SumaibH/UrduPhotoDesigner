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
import kotlin.math.roundToInt

@AndroidEntryPoint
class ColorPickerFragment : Fragment() {
    private var _binding: FragmentColorPickerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by activityViewModels()
    private var currentHue = 0f
    private var tempColor: Int = Color.RED

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
        val hueBar   = binding.seekbarHue
        val alphaBar = binding.seekbarAlpha

        val initialHue   = (hueBar.progress   * hueBar.max).roundToInt()
        val initialAlpha = (alphaBar.progress * alphaBar.max).roundToInt()

        hueBar.onProgressChanged  ?.invoke(initialHue)
        alphaBar.onProgressChanged?.invoke(initialAlpha)
        binding.back.setOnClickListener {
            viewModel.stopPicking()
            parentFragment?.childFragmentManager?.popBackStack()
        }
        binding.done.setOnClickListener {
            viewModel.finishPicking(tempColor)
            parentFragment?.childFragmentManager?.popBackStack()
        }
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

                val alphaInt = binding.seekbarAlpha.progress.toInt() * binding.seekbarAlpha.max
                binding.seekbarAlpha.progress = binding.seekbarAlpha.progress
                tempColor = bakeAlpha((alphaInt shl 24) or (opaque and 0x00FFFFFF))
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
                tempColor = bakeAlpha((alphaVal shl 24) or hsvRgb)
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