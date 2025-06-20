package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.databinding.FragmentFillStrokeBinding
import com.example.urduphotodesigner.ui.editor.panels.background.gradients.GradientsAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FillStrokeFragment : Fragment() {
    private var _binding: FragmentFillStrokeBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorsAdapter: ColorsAdapter
    private lateinit var gradientsAdapter: GradientsAdapter
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
        _binding = FragmentFillStrokeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupControlsVisibility()
        initObservers()
        setEvents()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            val selectedColor = color.colorCode.toColorInt()
            when (currentTab?.lowercase()) {
                "stroke" -> {
                    // preserve existing width
                    val width = viewModel.borderWidth.value ?: 1f
                    viewModel.clearStrokeGradients()
                    viewModel.setTextBorder(true, selectedColor, width)
                }
                else -> {
                    viewModel.clearFillGradients()
                    viewModel.setTextColor(selectedColor)
                }
            }
        },{
            when (currentTab?.lowercase()) {
                "stroke" -> {
                    // preserve existing width
                    viewModel.setTextBorder(false, android.R.color.transparent, 0f)
                }
                else -> viewModel.setTextColor(android.R.color.transparent)
            }
        }) {
            openColorPickerDialog()
        }

        gradientsAdapter = GradientsAdapter(
            gradientList = Constants.gradientList,
            onGradientSelected = { _, item ->
                when (currentTab?.lowercase()) {
                    "stroke" -> {
                        val colorsArray = item.colors.toIntArray()
                        val positions = FloatArray(colorsArray.size) { i ->
                            if (colorsArray.size == 1) 0f else i.toFloat() / (colorsArray.size - 1)
                        }
                        val width = viewModel.borderWidth.value ?: 1f
                        viewModel.setTextStrokeGradient(colorsArray, positions,width)
                    }
                    else -> {
                        val colorsArray = item.colors.toIntArray()
                        val positions   = FloatArray(colorsArray.size) { i ->
                            if (colorsArray.size == 1) 0f else i.toFloat() / (colorsArray.size - 1)
                        }
                        viewModel.setTextFillGradient(colorsArray, positions)
                    }
                }
            },
            onNoneSelected = {
                when (currentTab?.lowercase()) {
                    "stroke" -> {
                        viewModel.clearStrokeGradients()
                    }
                    else -> viewModel.clearFillGradients()
                }
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
    }

    private fun initObservers(){
        viewModel.currentTextSize.observe(viewLifecycleOwner) { size ->
            if (currentTab?.lowercase() == "text") {
                binding.fontSize.text = "${size?.toInt() ?: 40}"
                binding.font.progress = size?.toInt() ?: 40
            }
        }

        viewModel.borderWidth.observe(viewLifecycleOwner) { width ->
            if (currentTab?.lowercase() == "stroke") {
                binding.borderSize.text = "${width?.toInt() ?: 0}"
                binding.border.progress = width?.toInt() ?: 0
            }
        }

        viewModel.currentTextColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "fill") {
                colorsAdapter.selectedColor = color ?: Color.BLACK // Default to black if null
            }
        }

        viewModel.borderColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "stroke") {
                colorsAdapter.selectedColor = color ?: Color.BLACK
            }
        }

        viewModel.fillGradientColors.observe(viewLifecycleOwner) { colorsArray ->
            // once you know which colors are active, find the matching preset
            val match = Constants.gradientList.find {
                it.colors.toIntArray().contentEquals(colorsArray)
            }
            if (match != null) {
                gradientsAdapter.selectedItem = match
            }
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
                    "stroke" -> {
                        // preserve existing width
                        val width = viewModel.borderWidth.value ?: 1f
                        viewModel.setTextBorder(true, selectedColor, width)
                    }
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


    private fun setEvents() {
        // Font Size SeekBar
        binding.font.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser){
                        binding.fontSize.text = "$progress"
                        viewModel.setTextSize(progress.toFloat())
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.border.apply {
            min = 0
            max = 10
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser){
                        binding.borderSize.text = "$progress"
                        val color = viewModel.borderColor.value ?: Color.BLACK
                        viewModel.setTextBorder(true, color, progress.toFloat())
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
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
        }
    }

    private fun setupControlsVisibility() {
        // only show the relevant controls panel
        when (currentTab?.lowercase()) {
            "stroke" -> {
                // preserve existing width
                binding.borderCard.visibility = View.VISIBLE
                binding.borderSize.text = "${viewModel.borderWidth.value!!}"
                binding.border.progress = viewModel.borderWidth.value?.toInt()!!
            }
            else -> {
                binding.fontCard.visibility = View.VISIBLE
                binding.fontSize.text = "${viewModel.currentTextSize.value!!}"
                binding.font.progress = viewModel.currentTextSize.value?.toInt()!!
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_NAME = "tab_name"

        fun newInstance(tabName: String): FillStrokeFragment {
            val fragment = FillStrokeFragment()
            val args = Bundle()
            args.putString(ARG_TAB_NAME, tabName)
            fragment.arguments = args
            return fragment
        }
    }
}