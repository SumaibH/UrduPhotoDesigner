package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.enums.PickerTarget
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.data.model.ShapeItem
import com.example.urduphotodesigner.databinding.FragmentLabelsBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ShapesAdapter
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LabelsFragment : Fragment() {
    private var _binding: FragmentLabelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private lateinit var colorsAdapter: ColorsAdapter

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
        },{
            openColorPickerDialog()
        },{
            viewModel.startPicking(PickerTarget.LABEL)
        })

        binding.colors.apply {
            adapter = colorsAdapter
        }

        binding.labels.apply {
            adapter = shapesAdapter
        }
    }

    private fun openColorPickerDialog() {
        // Get the current text color from the ViewModel to set as the initial color in the picker
        val initialColor = viewModel.currentTextColor.value ?: Color.BLACK

        ColorPickerDialogBuilder
            .with(requireContext())
            .setTitle("Choose Color")
            .initialColor(initialColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE) // You can choose different wheel types
            .density(6) // Density of the color wheel
            .lightnessSliderOnly() // If you want only lightness slider
            .setPositiveButton("Select") { _, selectedColor, _ ->
                viewModel.setTextLabel(
                    true,
                    selectedColor,
                    viewModel.labelShape.value!!
                )
            }

            .setNegativeButton("Cancel") { _, _ ->
                // Do nothing or handle cancellation
            }
            .showColorEdit(true) // Show hex/rgb editor
            .setColorEditTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            ) // Set text color of the editor
            .build()
            .show()
    }

    private fun initObservers(){
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