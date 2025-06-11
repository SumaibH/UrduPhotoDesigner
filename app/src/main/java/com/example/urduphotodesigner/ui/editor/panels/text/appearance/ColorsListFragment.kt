package com.example.urduphotodesigner.ui.editor.panels.text.appearance

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
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.databinding.FragmentColorsListBinding
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorsListFragment : Fragment() {
    private var _binding: FragmentColorsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorsAdapter: ColorsAdapter
    private val viewModel: CanvasViewModel by activityViewModels()
    private var currentTab: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTab = arguments?.getString("tab_name")
    }

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
        initObservers()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            val selectedColor = color.colorCode.toColorInt()
            when (currentTab?.lowercase()) {
                "border" -> viewModel.setTextBorder(true, selectedColor, 6f) // border enabled, 6f width
                "shadow" -> viewModel.setTextShadow(true, selectedColor, 6f, 6f) // shadow with dx/dy
                "label" -> viewModel.setTextLabel(true, selectedColor, LabelShape.RECTANGLE)
                else -> viewModel.setTextColor(selectedColor) // default to text color
            }
        }) {
            openColorPickerDialog()
        }

        binding.colors.apply {
            adapter = colorsAdapter
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
                when (currentTab?.lowercase()) {
                    "border" -> viewModel.setTextBorder(true, selectedColor, 6f)
                    "shadow" -> viewModel.setTextShadow(true, selectedColor, 6f, 6f)
                    "label" -> viewModel.setTextLabel(true, selectedColor, LabelShape.RECTANGLE)
                    else -> viewModel.setTextColor(selectedColor)
                }
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

    private fun initObservers() {
        viewModel.currentTextColor.observe(viewLifecycleOwner) { color ->
            colorsAdapter.selectedColor = color!!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_NAME = "tab_name"

        fun newInstance(tabName: String): ColorsListFragment {
            val fragment = ColorsListFragment()
            val args = Bundle()
            args.putString(ARG_TAB_NAME, tabName)
            fragment.arguments = args
            return fragment
        }
    }
}