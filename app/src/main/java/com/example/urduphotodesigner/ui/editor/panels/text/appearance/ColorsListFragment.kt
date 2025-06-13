package com.example.urduphotodesigner.ui.editor.panels.text.appearance

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.data.model.ShapeItem
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
    private val shapesList = listOf(
        ShapeItem(LabelShape.RECTANGLE_FILL,R.drawable.ic_rect_fill),
        ShapeItem(LabelShape.RECTANGLE_STROKE,R.drawable.ic_rect_stroke),
        ShapeItem(LabelShape.OVAL_FILL,R.drawable.ic_oval_fill),
        ShapeItem(LabelShape.OVAL_STROKE,R.drawable.ic_oval_stroke),
        ShapeItem(LabelShape.CIRCLE_FILL,R.drawable.ic_circle_fill),
        ShapeItem(LabelShape.CIRCLE_STROKE,R.drawable.ic_circle_stroke)
    )

    private val shapesAdapter = ShapesAdapter(shapesList) { selectedShape ->
        // When a shape is selected, apply the label to the text element
        viewModel.setTextLabel(true, viewModel.currentTextLabelColor.value!!, selectedShape) // Use selected shape
    }

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

        setupControlsVisibility()
        initSeekBars()
        setupRecyclerView()
        initObservers()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList, { color ->
            val selectedColor = color.colorCode.toColorInt()
            when (currentTab?.lowercase()) {
                "border" -> {
                    // preserve existing width
                    val width = viewModel.currentTextBorderWidth.value ?: 1f
                    viewModel.setTextBorder(true, selectedColor, width)
                }
                "shadow" -> {
                    val dx = viewModel.currentTextShadowDx.value ?: 0f
                    val dy = viewModel.currentTextShadowDy.value ?: 0f
                    viewModel.setTextShadow(true, selectedColor, dx, dy, 1f)
                }
                "label" -> viewModel.setTextLabel(true, selectedColor,
                    viewModel.currentTextLabelShape.value!!
                )
                else -> viewModel.setTextColor(selectedColor)
            }
        },{
            when (currentTab?.lowercase()) {
                "border" -> {
                    // preserve existing width
                    viewModel.setTextBorder(false, android.R.color.transparent, 0f)
                }
                "shadow" -> {
                    val dx = viewModel.currentTextShadowDx.value ?: 0f
                    val dy = viewModel.currentTextShadowDy.value ?: 0f
                    viewModel.setTextShadow(false, android.R.color.transparent, dx, dy, 0f)
                }
                "label" -> viewModel.setTextLabel(false, android.R.color.transparent, viewModel.currentTextLabelShape.value!!)
                else -> viewModel.setTextColor(android.R.color.transparent)
            }
        }) {
            openColorPickerDialog()
        }

        binding.colors.apply {
            adapter = colorsAdapter
        }

        binding.shapes.apply {
            adapter = shapesAdapter
        }
    }

    private fun initSeekBars() {
        // Border width SeekBar
        binding.border.apply {
            min = 1
            max = 10
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.borderSize.text = "$progress"
                    val color = viewModel.currentTextBorderColor.value ?: Color.BLACK
                    viewModel.setTextBorder(true, color, progress.toFloat())
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        // Shadow DX SeekBar
        binding.shadowX.apply {
            min = 1
            max = 50
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.shadowXSize.text = "$progress"
                    val color = viewModel.currentTextShadowColor.value ?: Color.BLACK
                    val dy    = viewModel.currentTextShadowDy.value    ?: 0f
                    viewModel.setTextShadow(true, color, progress.toFloat(), dy, 1f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        // Shadow DY SeekBar
        binding.shadowY.apply {
            min = 1
            max = 50
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.shadowYSize.text = "$progress"
                    val color = viewModel.currentTextShadowColor.value ?: Color.BLACK
                    val dx    = viewModel.currentTextShadowDx.value    ?: 0f
                    viewModel.setTextShadow(true, color, dx, progress.toFloat(), 1f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        // Font Size SeekBar
        binding.font.apply {
            min = 1
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.fontSize.text = "$progress"
                    viewModel.setTextSize(progress.toFloat())
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    private fun setupControlsVisibility() {
        // only show the relevant controls panel
        when (currentTab?.lowercase()) {
            "border" -> {
                // preserve existing width
                binding.borderCard.visibility = View.VISIBLE
                binding.borderSize.text = "${viewModel.currentTextBorderWidth.value!!}"
                binding.border.progress = viewModel.currentTextBorderWidth.value?.toInt()!!
            }
            "shadow" -> {
                binding.shadowXCard.visibility = View.VISIBLE
                binding.shadowYCard.visibility = View.VISIBLE
                binding.shadowXSize.text = "${viewModel.currentTextShadowDx.value!!}"
                binding.shadowYSize.text = "${viewModel.currentTextShadowDy.value!!}"
                binding.shadowX.progress = viewModel.currentTextShadowDx.value?.toInt()!!
                binding.shadowY.progress = viewModel.currentTextShadowDy.value?.toInt()!!
            }
            "label" -> {
                binding.shapes.visibility = View.VISIBLE
                shapesAdapter.selectedShape = viewModel.currentTextLabelShape.value!!
            }
            else -> {
                binding.fontCard.visibility = View.VISIBLE
                binding.fontSize.text = "${viewModel.currentTextSize.value!!}"
                binding.font.progress = viewModel.currentTextSize.value?.toInt()!!
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
                    "border" -> {
                        // preserve existing width
                        val width = viewModel.currentTextBorderWidth.value ?: 1f
                        viewModel.setTextBorder(true, selectedColor, width)
                    }
                    "shadow" -> {
                        val dx = viewModel.currentTextShadowDx.value ?: 0f
                        val dy = viewModel.currentTextShadowDy.value ?: 0f
                        viewModel.setTextShadow(true, selectedColor, dx, dy, 1f)
                    }
                    "label" -> viewModel.setTextLabel(true, selectedColor, viewModel.currentTextLabelShape.value!!)
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
        // Observe text color only if the current tab is "text"
        viewModel.currentTextColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "text") {
                colorsAdapter.selectedColor = color ?: Color.BLACK // Default to black if null
            }
        }

        // Observe border color only if the current tab is "border"
        viewModel.currentTextBorderColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "border") {
                colorsAdapter.selectedColor = color ?: Color.BLACK // Default to black if null
            }
        }

        // Observe shadow color only if the current tab is "shadow"
        viewModel.currentTextShadowColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "shadow") {
                colorsAdapter.selectedColor = color ?: Color.BLACK // Default to black if null
            }
        }

        // Observe label color only if the current tab is "label"
        viewModel.currentTextLabelColor.observe(viewLifecycleOwner) { color ->
            if (currentTab?.lowercase() == "label") {
                colorsAdapter.selectedColor = color ?: Color.BLACK // Default to black if null
            }
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