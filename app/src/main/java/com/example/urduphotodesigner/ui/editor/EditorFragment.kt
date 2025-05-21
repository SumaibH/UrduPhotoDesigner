package com.example.urduphotodesigner.ui.editor

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            if (Build.MANUFACTURER.equals("realme", ignoreCase = true)) {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(bottom = systemBars.bottom)
            }
            insets
        }

        canvasSize = arguments?.getSerializable("canvas_size") as CanvasSize
        currentUnit = (arguments?.getSerializable("unit_type") as? UnitType)!!

        setEvents()
        observeViewModel()
    }

    private fun showTextEditDialog(element: CanvasElement) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_text)

        val editText = dialog.findViewById<EditText>(R.id.edit_text_input)
        val done = dialog.findViewById<ImageView>(R.id.done)
        editText.setText(element.text)
        editText.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        done.setOnClickListener {
            val newText = editText.text.toString()
            if (newText.isNotBlank()) {
                element.text = newText
                viewModel.updateText(element)
            }
            dialog.dismiss()
        }

        // Set dialog window attributes for no dim background
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent) // Make background transparent
            setDimAmount(0f) // No dim
            setGravity(Gravity.BOTTOM)
            // You might want to adjust width/height if the layout doesn't fill as expected
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Show the dialog
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
            bitmap?.let { canvasManager.setCanvasBackgroundImage(it) }
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
            canvasManager.setTextColor(color!!)
        })

        viewModel.currentTextSize.observe(viewLifecycleOwner, Observer { size ->
            canvasManager.setTextSize(size!!)
        })

        viewModel.currentTextAlignment.observe(viewLifecycleOwner, Observer { alignment ->
            canvasManager.setTextAlignment(alignment!!)
        })

        viewModel.currentTextOpacity.observe(viewLifecycleOwner, Observer { opacity ->
            canvasManager.setOpacity(opacity!!)
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
                },onElementSelected = { elements ->
                    viewModel.setSelectedElementsFromLayers(elements)
                    viewModel.onCanvasSelectionChanged(elements)
                    if (elements.isNotEmpty()) {
                        binding.opacityBox.visibility = View.VISIBLE // Make opacity panel visible
                    } else {
                        binding.opacityBox.visibility = View.GONE // Hide opacity panel
                    }
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