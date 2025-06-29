package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.enums.PickerTarget
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.data.model.ShapeItem
import com.example.urduphotodesigner.databinding.FragmentLabelsBinding
import com.example.urduphotodesigner.ui.editor.panels.background.gradients.GradientsAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ShapesAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient.ColorPickerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LabelsFragment : Fragment() {
    private var _binding: FragmentLabelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private lateinit var colorsAdapter: ColorsAdapter
    private lateinit var gradientsAdapter: GradientsAdapter

    private val shapesList = listOf(
        ShapeItem(LabelShape.RECTANGLE_FILL, R.drawable.ic_rect_fill),
        ShapeItem(LabelShape.RECTANGLE_STROKE, R.drawable.ic_rect_stroke),
        ShapeItem(LabelShape.OVAL_FILL, R.drawable.ic_oval_fill),
        ShapeItem(LabelShape.OVAL_STROKE, R.drawable.ic_oval_stroke),
        ShapeItem(LabelShape.CIRCLE_FILL, R.drawable.ic_circle_fill),
        ShapeItem(LabelShape.CIRCLE_STROKE, R.drawable.ic_circle_stroke)
    )

    private val shapesAdapter = ShapesAdapter(shapesList) { selectedShape ->
        // When a shape is selected, apply the label to the text element
        viewModel.setTextLabel(
            true,
            viewModel.labelColor.value!!,
            selectedShape
        ) // Use selected shape
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabelsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        initObservers()
    }

    private fun setEvents() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            val selectedColor = color.colorCode.toColorInt()
            viewModel.clearLabelGradients()
            viewModel.setTextLabel(
                true, selectedColor,
                viewModel.labelShape.value!!
            )
        }, {
            viewModel.setTextLabel(
                false,
                android.R.color.transparent,
                viewModel.labelShape.value!!
            )
        }, {
            viewModel.clearLabelGradients()
            viewModel.startPicking(PickerTarget.COLOR_PICKER_LABEL)
            childFragmentManager
                .beginTransaction()
                .replace(R.id.labelsFragment, ColorPickerFragment())
                .addToBackStack(null)
                .commit()
        }, {
            viewModel.clearLabelGradients()
            viewModel.startPicking(PickerTarget.EYE_DROPPER_LABEL)
        })

        gradientsAdapter = GradientsAdapter(
            gradientList = Constants.gradientList,
            onGradientSelected = { _, item ->
                val colorsArray = item.colors.toIntArray()
                val positions = FloatArray(colorsArray.size) { i ->
                    if (colorsArray.size == 1) 0f else i.toFloat() / (colorsArray.size - 1)
                }
                val labelShape = viewModel.labelShape.value ?: LabelShape.RECTANGLE_FILL
                viewModel.setTextLabelGradient(true, labelShape, colorsArray, positions)
            },
            onNoneSelected = {
               viewModel.clearLabelGradients()
            },
            onGradientPickerClicked = {
//                openColorPickerDialog()
            }
        )

        binding.colors.apply {
            adapter = colorsAdapter
        }

        binding.gradients.apply {
            adapter = gradientsAdapter
        }

        binding.labels.apply {
            adapter = shapesAdapter
        }

        binding.solid.setOnClickListener {
            if (!binding.colors.isVisible) {
                togglePanels()
            }
        }

        binding.gradient.setOnClickListener {
            if (!binding.gradients.isVisible) {
                togglePanels()
            }
        }
    }

    private fun togglePanels() {
        val fadeDuration = 300L

        // Check if clicked panel is already visible; if so, do nothing.
        if (binding.colors.isVisible && binding.gradients.isVisible) return

        // Check which panel is visible and apply transition
        val showGradients = binding.gradients.isVisible

        // If gradient is visible, hide it and show solid; otherwise, do the opposite
        if (showGradients) {
            // Fade out gradient and hide it
            binding.gradients.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .withEndAction {
                    binding.gradients.visibility = View.GONE
                    // Now fade in solid after gradient is hidden
                    binding.colors.alpha = 0f
                    binding.colors.visibility = View.VISIBLE
                    binding.colors.animate()
                        .alpha(1f)
                        .setDuration(fadeDuration)
                        .start()
                }
                .start()

            binding.gradient.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.contrast))
            binding.solid.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            // Fade out solid and hide it
            binding.colors.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .withEndAction {
                    binding.colors.visibility = View.GONE
                    // Now fade in gradient after solid is hidden
                    binding.gradients.alpha = 0f
                    binding.gradients.visibility = View.VISIBLE
                    binding.gradients.animate()
                        .alpha(1f)
                        .setDuration(fadeDuration)
                        .start()
                }
                .start()
            binding.gradient.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            binding.solid.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.contrast))
        }
    }

    private fun initObservers() {
        viewModel.labelColor.observe(viewLifecycleOwner) { color ->
            colorsAdapter.selectedColor = color ?: Color.BLACK
        }

        viewModel.labelShape.observe(viewLifecycleOwner) { shape ->
            shapesAdapter.selectedShape = shape
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPicking()
    }

    companion object {
        fun newInstance(): LabelsFragment {
            val fragment = LabelsFragment()
            return fragment
        }
    }
}