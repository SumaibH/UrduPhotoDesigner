package com.example.urduphotodesigner.ui.editor

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.Converter.cmToPx
import com.example.urduphotodesigner.common.Converter.inchesToPx
import com.example.urduphotodesigner.common.canvas.CanvasElement
import com.example.urduphotodesigner.common.canvas.CanvasManager
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.enums.UnitType
import com.example.urduphotodesigner.common.views.SizedCanvasView
import com.example.urduphotodesigner.data.model.CanvasSize
import com.example.urduphotodesigner.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorFragment : Fragment() {
    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var canvasManager: CanvasManager
    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private lateinit var canvasSize: CanvasSize
    private var currentUnit = UnitType.PIXELS

    private val viewModel: CanvasViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        canvasSize = arguments?.getSerializable("canvas_size") as CanvasSize
        currentUnit = (arguments?.getSerializable("unit_type") as? UnitType)!!

        setEvents()
        observeViewModel()
    }

    private fun showTextEditDialog(element: CanvasElement) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView =
            inflater.inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text_input)

        builder.setView(dialogView)
        editText.setText(element.text)
        builder.setTitle("Add Text")
        builder.setPositiveButton("Save") { dialog, _ ->
            val newText = editText.text.toString()
            if (newText.isNotBlank()) {
                element.text = newText
                viewModel.updateText(element)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.canvasElements.observe(viewLifecycleOwner, Observer { elements ->
            canvasManager.syncElements(elements)
            binding.canvasContainer.invalidate()
        })

        viewModel.backgroundColor.observe(viewLifecycleOwner, Observer { color ->
            canvasManager.setCanvasBackgroundColor(color)
        })

        viewModel.canUndo.observe(viewLifecycleOwner) { canUndo ->
            binding.undo.isEnabled = canUndo
        }

        viewModel.canRedo.observe(viewLifecycleOwner) { canRedo ->
            binding.redo.isEnabled = canRedo
        }

        viewModel.backgroundImage.observe(viewLifecycleOwner, Observer { bitmap ->
            canvasManager.setCanvasBackgroundImage(bitmap!!)
        })

        viewModel.backgroundGradient.observe(viewLifecycleOwner, Observer { gradient ->
            gradient?.let { (colors, positions) ->
                canvasManager.setCanvasBackgroundGradient(colors ?: intArrayOf(), positions)
            }
        })

        viewModel.currentFont.observe(viewLifecycleOwner, Observer { font ->
            if (font != null) {
                canvasManager.setFont(font)
            }
        })

        viewModel.currentTextColor.observe(viewLifecycleOwner, Observer { color ->
            canvasManager.setTextColor(color)
        })

        viewModel.currentTextSize.observe(viewLifecycleOwner, Observer { size ->
            canvasManager.setTextSize(size)
        })

        viewModel.currentTextAlignment.observe(viewLifecycleOwner, Observer { alignment ->
            canvasManager.setTextAlignment(alignment)
        })

        viewModel.currentTextOpacity.observe(viewLifecycleOwner, Observer { opacity ->
            canvasManager.setOpacity(opacity)
        })
    }

    private fun setEvents() {

        binding.back.setOnClickListener { findNavController().navigateUp() }

        val widthPx = when (currentUnit) {
            UnitType.INCHES -> inchesToPx(canvasSize.width)
            UnitType.CENTIMETERS -> cmToPx(canvasSize.width)
            UnitType.PIXELS -> canvasSize.width.toInt()
        }

        val heightPx = when (currentUnit) {
            UnitType.INCHES -> inchesToPx(canvasSize.height)
            UnitType.CENTIMETERS -> cmToPx(canvasSize.height)
            UnitType.PIXELS -> canvasSize.height.toInt()
        }

        canvasManager = CanvasManager(
            SizedCanvasView(
                requireContext(),
                canvasWidth = widthPx,
                canvasHeight = heightPx,
                onEditTextRequested = { element ->
                    showTextEditDialog(element)
                },
                onElementChanged = { canvasElement ->
                    viewModel.canvasElements.value?.find { it.id == canvasElement.id }?.let {
                        viewModel.updateElement(canvasElement)
                    }
                },
                onElementRemoved = { canvasElement ->
                    viewModel.canvasElements.value?.find { it.id == canvasElement.id }?.let {
                        viewModel.removeElement(it)
                    }
                },onElementSelected = { element ->
                    viewModel.setSelectedElement(element)
                }
            ).apply {
                binding.canvasContainer.addView(this)
            }
        )

        // Setup navigation
        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.panelNavHost) as NavHostFragment
        _navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_background -> navController.navigate(R.id.backgroundsFragment)
                R.id.nav_objects -> navController.navigate(R.id.objectsFragment)
                R.id.nav_text -> navController.navigate(R.id.textFragment)
                R.id.nav_images -> navController.navigate(R.id.imagesFragment)
                R.id.nav_layers -> navController.navigate(R.id.layersFragment)
                else -> false
            }
            true
        }

        binding.undo.setOnClickListener { viewModel.undo() }
        binding.redo.setOnClickListener { viewModel.redo() }
    }

    override fun onDestroy() {
        super.onDestroy()
        _navController = null
        _binding = null
    }
}