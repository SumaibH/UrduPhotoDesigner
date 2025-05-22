package com.example.urduphotodesigner.ui.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.Converter.cmToPx
import com.example.urduphotodesigner.common.Converter.inchesToPx
import com.example.urduphotodesigner.common.Converter.pxToCm
import com.example.urduphotodesigner.common.Converter.pxToInches
import com.example.urduphotodesigner.common.enums.UnitType
import com.example.urduphotodesigner.common.canvas.CanvasSize
import com.example.urduphotodesigner.databinding.FragmentCreateBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateFragment : Fragment() {
    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

    private val unitList = listOf("Pixels", "Inches", "Centimeters")

    private val sizeList = listOf(
        CanvasSize("Billboard", R.drawable.ic_bill_board, 1920f, 1080f),
        CanvasSize("Vertical Banner", R.drawable.ic_vertical_banner, 1080f, 1920f),
        CanvasSize("Horizontal Banner", R.drawable.ic_horizontal_banner, 1920f, 600f),
        CanvasSize("A4", R.drawable.ic_a_four, 2480f, 3508f),
        CanvasSize("Letter", R.drawable.ic_letter, 2550f, 3300f)
    )
    private var currentUnit = UnitType.PIXELS
    private var isLinked = false
    private var aspectRatio: Float? = null

    private lateinit var adapter: CanvasSizeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
    }

    private fun setEvents() {
        binding.apply {
            unitBox.setOnClickListener {
                spinner.rotation = 180f
                val popup = PopupMenu(requireContext(), unit)
                unitList.forEachIndexed { index, unit ->
                    popup.menu.add(Menu.NONE, index, index, unit)
                }
                popup.setOnMenuItemClickListener { menuItem ->
                    val selectedUnitStr = unitList[menuItem.itemId]
                    unit.text = selectedUnitStr

                    // Convert old width/height to pixels first (if not PIXELS)
                    val oldWidthPx = when(currentUnit) {
                        UnitType.PIXELS -> getSafeIntValue(width)
                        UnitType.INCHES -> inchesToPx(width.text.toString().toFloatOrNull() ?: 1f)
                        UnitType.CENTIMETERS -> cmToPx(width.text.toString().toFloatOrNull() ?: 1f)
                    }
                    val oldHeightPx = when(currentUnit) {
                        UnitType.PIXELS -> getSafeIntValue(height)
                        UnitType.INCHES -> inchesToPx(height.text.toString().toFloatOrNull() ?: 1f)
                        UnitType.CENTIMETERS -> cmToPx(height.text.toString().toFloatOrNull() ?: 1f)
                    }

                    // Update current unit
                    currentUnit = when(selectedUnitStr) {
                        "Pixels" -> UnitType.PIXELS
                        "Inches" -> UnitType.INCHES
                        "Centimeters" -> UnitType.CENTIMETERS
                        else -> UnitType.PIXELS
                    }

                    // Convert pixel values to selected unit for UI display
                    when(currentUnit) {
                        UnitType.PIXELS -> {
                            width.setText(oldWidthPx.toString())
                            height.setText(oldHeightPx.toString())
                        }
                        UnitType.INCHES -> {
                            width.setText(String.format("%.1f", pxToInches(oldWidthPx.toFloat())))
                            height.setText(String.format("%.1f", pxToInches(oldHeightPx.toFloat())))
                        }
                        UnitType.CENTIMETERS -> {
                            width.setText(String.format("%.1f", pxToCm(oldWidthPx.toFloat())))
                            height.setText(String.format("%.1f", pxToCm(oldHeightPx.toFloat())))
                        }
                    }

                    // Update the RecyclerView list with converted sizes
                    updateListForUnit(currentUnit)

                    true
                }
                popup.setOnDismissListener {
                    spinner.rotation = 0f
                }
                popup.show()
            }

            adapter = CanvasSizeAdapter(sizeList) { selected ->
                val bundle = Bundle().apply {
                    putSerializable("canvas_size", selected)
                    putSerializable("unit_type", currentUnit)
                }

                findNavController().navigate(R.id.editorFragment, bundle)
            }
            sizesRV.adapter = adapter

            incWidth.setOnClickListener {
                val newWidth = getSafeIntValue(width) + 1
                width.setText(newWidth.toString())

                if (isLinked && aspectRatio != null) {
                    val newHeight = (newWidth / aspectRatio!!).toInt()
                    height.setText(newHeight.toString())
                }
            }

            decWidth.setOnClickListener {
                val current = getSafeIntValue(width)
                if (current > 1) {
                    val newWidth = current - 1
                    width.setText(newWidth.toString())

                    if (isLinked && aspectRatio != null) {
                        val newHeight = (newWidth / aspectRatio!!).toInt()
                        height.setText(newHeight.toString())
                    }
                }
            }

            incHeight.setOnClickListener {
                val newHeight = getSafeIntValue(height) + 1
                height.setText(newHeight.toString())

                if (isLinked && aspectRatio != null) {
                    val newWidth = (newHeight * aspectRatio!!).toInt()
                    width.setText(newWidth.toString())
                }
            }

            decHeight.setOnClickListener {
                val current = getSafeIntValue(height)
                if (current > 1) {
                    val newHeight = current - 1
                    height.setText(newHeight.toString())

                    if (isLinked && aspectRatio != null) {
                        val newWidth = (newHeight * aspectRatio!!).toInt()
                        width.setText(newWidth.toString())
                    }
                }
            }

            link.setOnClickListener {
                isLinked = !isLinked
                link.setImageResource(
                    if (isLinked) R.drawable.ic_link else R.drawable.ic_unlink
                )

                if (isLinked) {
                    val widthVal = getSafeIntValue(width)
                    val heightVal = getSafeIntValue(height)
                    if (heightVal != 0.toFloat()) {
                        aspectRatio = widthVal / heightVal
                    }
                } else {
                    aspectRatio = null
                }
            }

            // Create button click
            binding.create.setOnClickListener {
                val widthText = width.text.toString().trim()
                val heightText = height.text.toString().trim()

                if (widthText.isEmpty() || widthText.toFloatOrNull() == 0f ) {
                    Snackbar.make(binding.root, "Width cannot be 0", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (heightText.isEmpty() || heightText.toFloatOrNull() == 0f){
                    Snackbar.make(binding.root, "Height cannot be 0", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val widthVal = getSafeIntValue(width)
                val heightVal = getSafeIntValue(height)

                val canvasSize = CanvasSize("Custom", 0, widthVal, heightVal)
                val bundle = Bundle().apply {
                    putSerializable("canvas_size", canvasSize)
                    putSerializable("unit_type", currentUnit)
                }

                findNavController().navigate(R.id.editorFragment, bundle)
            }

            back.setOnClickListener { findNavController().navigateUp() }
        }
    }

    private fun updateListForUnit(unitType: UnitType) {
        val convertedList = sizeList.map { size ->
            when(unitType) {
                UnitType.PIXELS -> size.copy()
                UnitType.INCHES -> size.copy(
                    width = String.format("%.1f", pxToInches(size.width)).toFloat(),
                    height = String.format("%.1f", pxToInches(size.height)).toFloat()
                )
                UnitType.CENTIMETERS -> size.copy(
                    width = String.format("%.1f", pxToCm(size.width)).toFloat(),
                    height = String.format("%.1f", pxToCm(size.height)).toFloat()
                )
            }
        }
        adapter.submitList(convertedList)
    }

    private fun getSafeIntValue(editText: EditText): Float {
        return editText.text.toString().toFloatOrNull()?.coerceAtLeast(1f) ?: 1f
    }

    override fun onResume() {
        super.onResume()
        // Assuming you have a TextView in your layout called unitTextView
        binding.unit.text = when (currentUnit) {
            UnitType.INCHES -> "Inches"
            UnitType.CENTIMETERS -> "Centimeters"
            UnitType.PIXELS -> "Pixels"
        }
        binding.link.setImageResource(
            if (isLinked) R.drawable.ic_link else R.drawable.ic_unlink
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}