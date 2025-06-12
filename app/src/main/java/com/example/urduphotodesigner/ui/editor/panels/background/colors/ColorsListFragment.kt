package com.example.urduphotodesigner.ui.editor.panels.background.colors

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentColorsListBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.ColorsAdapter
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorsListFragment : Fragment() {
    private var _binding: FragmentColorsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorsAdapter: ColorsAdapter
    private val viewModel: CanvasViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            viewModel.setCanvasBackgroundColor(color.colorCode.toColorInt())
        },{
            viewModel.setCanvasBackgroundColor(android.R.color.transparent)
        }) {
            // This is the lambda for when the color picker is clicked
            openColorPickerDialog()
        }
        binding.colors.apply {
            adapter = colorsAdapter
        }
    }

    private fun openColorPickerDialog() {
        // Get the current text color from the ViewModel to set as the initial color in the picker
        val initialColor = viewModel.currentTextColor.value ?: Color.WHITE

        ColorPickerDialogBuilder
            .with(requireContext())
            .setTitle("Choose Color")
            .initialColor(initialColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE) // You can choose different wheel types
            .density(6) // Density of the color wheel
            .lightnessSliderOnly() // If you want only lightness slider
            .setPositiveButton("Select") { _, selectedColor, _ ->
                viewModel.setCanvasBackgroundColor(selectedColor)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Do nothing or handle cancellation
            }
            .showColorEdit(true) // Show hex/rgb editor
            .setColorEditTextColor(ContextCompat.getColor(requireContext(), R.color.whiteText)) // Set text color of the editor
            .build()
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): ColorsListFragment {
            return ColorsListFragment()
        }
    }
}