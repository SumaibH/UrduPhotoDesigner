package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.PickerTarget
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.databinding.FragmentGradientColorListBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradientColorListFragment : Fragment() {
    private var _binding: FragmentGradientColorListBinding? = null
    private val binding get() = _binding!!
    private lateinit var colorsAdapter: ColorsAdapter
    private val viewModel: CanvasViewModel by activityViewModels()
    private var selectedColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradientColorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { _, _ -> true }
        setupRecyclerView()
        initObserver()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            selectedColor = color.colorCode.toColorInt()
            colorsAdapter.selectedColor = selectedColor
        }, {
            selectedColor = android.R.color.transparent
        }, {
            viewModel.startPicking(PickerTarget.COLOR_PICKER_GRADIENT)
            childFragmentManager
                .beginTransaction()
                .replace(R.id.gradientColorFragment, ColorPickerFragment())
                .addToBackStack(null)
                .commit()
        }, {
            viewModel.startPicking(PickerTarget.EYE_DROPPER_GRADIENT)
        })

        binding.colors.apply {
            adapter = colorsAdapter
        }

        binding.back.setOnClickListener {
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        binding.done.setOnClickListener {
            viewModel.updateSelectedStopColor(selectedColor)
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        binding.delete.setOnClickListener {
            viewModel.removeSelectedStop()
            parentFragment
                ?.childFragmentManager
                ?.popBackStack()
        }

        binding.opacity.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, alpha: Int, fromUser: Boolean) {
                if (fromUser) {
                    // extract RGB from the original stop color
                    val rgb = selectedColor and 0x00FFFFFF
                    binding.opacitySize.text = "$alpha"
                    // bake new alpha in front of that
                    selectedColor = bakeAlpha((alpha shl 24) or rgb)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun initObserver() {
        viewModel.gradientStopColor.observe(viewLifecycleOwner) { color ->
            selectedColor = color
            colorsAdapter.selectedColor = selectedColor
            val alpha = Color.alpha(selectedColor)
            binding.opacity.progress = alpha
        }
    }

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
        _binding = null
    }

}