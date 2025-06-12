package com.example.urduphotodesigner.ui.editor

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.common.utils.Converter.cmToPx
import com.example.urduphotodesigner.common.utils.Converter.inchesToPx
import com.example.urduphotodesigner.common.canvas.CanvasManager
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.model.CanvasSize
import com.example.urduphotodesigner.common.canvas.enums.UnitType
import com.example.urduphotodesigner.common.views.SizedCanvasView
import com.example.urduphotodesigner.databinding.BottomSheetExportSettingsBinding
import com.example.urduphotodesigner.databinding.FragmentEditorBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

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
    private var currentPanelItemId: Int? = null

    private lateinit var sizedCanvasView: SizedCanvasView

    private var requestPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, proceed with export
            exportCanvasInternal()
        } else {
            // Permission denied, show a message to the user
            Toast.makeText(
                requireContext(),
                "Permission denied to save image. Please grant storage permission in settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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

        if (Constants.TEMPLATE.isNotEmpty()) {
            viewModel.loadTemplate(Constants.TEMPLATE, requireContext())
            Toast.makeText(requireContext(), "Template loaded!", Toast.LENGTH_SHORT).show()
        }
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
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        // Show the dialog
        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.canvasSize.observe(viewLifecycleOwner) { size ->
            canvasSize = size
            binding.canvasContainer.invalidate()
        }

        viewModel.canvasElements.observe(viewLifecycleOwner) { elements ->
            canvasManager.syncElements(elements)
            binding.canvasContainer.invalidate()
        }

        viewModel.backgroundColor.observe(viewLifecycleOwner) { color ->
            canvasManager.setCanvasBackgroundColor(color)
        }

        viewModel.canUndo.observe(viewLifecycleOwner) { canUndo ->
            binding.undo.isEnabled = canUndo
        }

        viewModel.canRedo.observe(viewLifecycleOwner) { canRedo ->
            binding.redo.isEnabled = canRedo
        }

        viewModel.backgroundImage.observe(viewLifecycleOwner) { bitmap ->
            bitmap?.let { canvasManager.setCanvasBackgroundImage(it) }
        }

        viewModel.backgroundGradient.observe(viewLifecycleOwner) { gradient ->
            gradient?.let { (colors, positions) ->
                canvasManager.setCanvasBackgroundGradient(colors ?: intArrayOf(), positions)
            }
        }

        viewModel.currentFont.observe(viewLifecycleOwner) { font ->
            if (font != null && viewModel.isExplicitChange()) {
            font.let { canvasManager.setFont(it) }
            }
        }

        viewModel.currentTextColor.observe(viewLifecycleOwner) { color ->
            canvasManager.setTextColor(color!!)
        }

        viewModel.currentTextSize.observe(viewLifecycleOwner) { size ->
            canvasManager.setTextSize(size!!)
        }

        viewModel.currentTextAlignment.observe(viewLifecycleOwner) { alignment ->
            canvasManager.setTextAlignment(alignment!!)
        }

        viewModel.currentImageFilter.observe(viewLifecycleOwner) { filter ->
            if (filter != null && viewModel.isExplicitChange()){
                canvasManager.applyImageFilter(filter)
            }
        }

        viewModel.currentTextOpacity.observe(viewLifecycleOwner) { opacity ->
            binding.seekBar.value = opacity?.toFloat()!!
            canvasManager.setOpacity(opacity)
        }
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

        // Setup navigation
        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.panelNavHost) as NavHostFragment
        _navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            if (currentPanelItemId == menuItem.itemId) {
                // Reselected the same item, hide the panel
                binding.panelNavHost.visibility = View.GONE
                currentPanelItemId = null // Reset current item
            } else {
                // New item selected, show the panel and navigate
                binding.panelNavHost.visibility = View.VISIBLE
                currentPanelItemId = menuItem.itemId

                when (menuItem.itemId) {
                    R.id.nav_background -> navController.navigate(R.id.backgroundsFragment)
                    R.id.nav_objects -> navController.navigate(R.id.objectsFragment)
                    R.id.nav_text -> navController.navigate(R.id.textFragment)
                    R.id.nav_images -> navController.navigate(R.id.imagesFragment)
                    R.id.nav_layers -> navController.navigate(R.id.layersFragment)
                    else -> false // Should not happen with defined menu items
                }
            }
            true
        }

        sizedCanvasView = SizedCanvasView(
            requireContext(),
            canvasWidth = widthPx,
            canvasHeight = heightPx,
            onEditTextRequested = { element ->
                if (element.type == ElementType.IMAGE){
                    viewModel.canvasElements.value?.find { it.id == element.id }?.let {
                        navController.navigate(R.id.filtersFragment)
                    }
                }else{
                    showTextEditDialog(element)
                }
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
            }, onElementSelected = { elements ->
                viewModel.setSelectedElementsFromLayers(elements)
                viewModel.onCanvasSelectionChanged(elements)

                binding.seekBar.visibility = View.INVISIBLE
            },
            onEndBatchUpdate = { elementId ->
                viewModel.endBatchUpdate(elementId)
            },
            onStartBatchUpdate = { elementId, actionType ->
                viewModel.startBatchUpdate(elementId, actionType)
            }
        ).apply {
            binding.canvasContainer.addView(this)
        }

        canvasManager = CanvasManager(sizedCanvasView)

        binding.undo.setOnClickListener { viewModel.undo() }
        binding.redo.setOnClickListener { viewModel.redo() }

        binding.opacityIcon.setOnClickListener {
            binding.seekBar.visibility =
                if (binding.seekBar.isVisible) View.INVISIBLE else View.VISIBLE
        }

        binding.seekBar.addOnChangeListener { _, value, _ ->
            viewModel.setOpacity(value.roundToInt())
        }

        binding.done.setOnClickListener {
            showExportSettingsDialog()
        }
    }

    private fun showExportSettingsDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = BottomSheetExportSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val currentOptions = viewModel.exportOptions.value ?: return
        val previewBitmap = sizedCanvasView.exportCanvasToBitmap(currentOptions)
        dialogBinding.previewImage.setImageBitmap(previewBitmap)

        val availableResolutions = viewModel.exportResolutions
        val currentExportOptions = viewModel.exportOptions.value // Get current (default or selected) options

        // Populate Resolution RadioGroup
        dialogBinding.radioGroupResolution.removeAllViews()
        availableResolutions.forEachIndexed { index, resolution ->
            val radioButton = RadioButton(requireContext()).apply {
                text = resolution.name
                id = index // Use index as ID for easy mapping
                isChecked = resolution == currentExportOptions!!.resolution // Set checked based on current options
            }
            dialogBinding.radioGroupResolution.addView(radioButton)
        }

        // Populate Quality RadioGroup
        val qualityOptions = mapOf(
            "High" to 90,
            "Medium" to 70,
            "Low" to 50
        )
        dialogBinding.radioGroupQuality.removeAllViews()
        qualityOptions.forEach { (name, value) ->
            val radioButton = RadioButton(requireContext()).apply {
                text = name
                id = value // Use quality value as ID
                isChecked = value == currentExportOptions!!.quality // Set checked based on current options
            }
            dialogBinding.radioGroupQuality.addView(radioButton)
        }

        // Populate Format RadioGroup
        val formatOptions = mapOf(
            "PNG" to Bitmap.CompressFormat.PNG,
            "JPEG" to Bitmap.CompressFormat.JPEG,
            "WEBP" to Bitmap.CompressFormat.WEBP
        )
        dialogBinding.radioGroupFormat.removeAllViews()
        formatOptions.forEach { (name, value) ->
            val radioButton = RadioButton(requireContext()).apply {
                text = name
                // Use a unique ID, or directly store the CompressFormat as a tag if using a custom listener
                id = if (value == Bitmap.CompressFormat.PNG) 0 else 1 // Arbitrary IDs
                isChecked = value == currentExportOptions!!.format // Set checked based on current options
            }
            dialogBinding.radioGroupFormat.addView(radioButton)
        }

        // Set listeners to update ViewModel's exportOptions
        dialogBinding.radioGroupResolution.setOnCheckedChangeListener { _, checkedId ->
            val selectedResolution = availableResolutions[checkedId]
            viewModel.updateExportOptions(
                currentExportOptions!!.copy(resolution = selectedResolution)
            )
        }

        dialogBinding.radioGroupQuality.setOnCheckedChangeListener { _, checkedId ->
            // Use checkedId directly as it's the quality value
            val selectedQuality = checkedId
            viewModel.updateExportOptions(
                currentExportOptions!!.copy(quality = selectedQuality)
            )
        }

        dialogBinding.radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            val selectedFormat = if (checkedId == 0) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            viewModel.updateExportOptions(
                currentExportOptions!!.copy(format = selectedFormat)
            )
        }

        dialogBinding.buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonExport.setOnClickListener {
            exportCanvas()
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun saveBitmapToGallery(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = requireContext().contentResolver
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, when (format) {
                Bitmap.CompressFormat.JPEG -> "image/jpeg"
                Bitmap.CompressFormat.PNG -> "image/png"
                Bitmap.CompressFormat.WEBP -> "image/webp"
                else -> "image/png"
            })
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(imageCollection, imageDetails)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(format, quality, out)
            }

            imageDetails.clear()
            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, imageDetails, null, null)
        }

        uri
    }

    private fun exportCanvas() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 and below
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                exportCanvasInternal()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else { // Android 10 (API 29) and above: No need for WRITE_EXTERNAL_STORAGE
            exportCanvasInternal()
        }
    }

    private fun exportCanvasInternal() {
        //Save Template
        viewModel.saveTemplate()

        // Retrieve the current export options from the ViewModel, which now includes the selected quality and format
        val currentExportOptions = viewModel.exportOptions.value

        lifecycleScope.launch(Dispatchers.IO) { // Use IO dispatcher for file operations
            val exportedBitmap = currentExportOptions.let { it?.let { it1 ->
                sizedCanvasView.exportCanvasToBitmap(
                    it1
                )
            } }

            if (exportedBitmap != null) {
                // Determine output file path and format
                val fileName = "exported_image_${System.currentTimeMillis()}"
                val fileExtension = when (currentExportOptions!!.format) {
                    Bitmap.CompressFormat.JPEG -> ".jpg"
                    Bitmap.CompressFormat.PNG -> ".png"
                    Bitmap.CompressFormat.WEBP -> ".webp"
                    else -> ".png" // Default to PNG
                }
                val outputPath = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "$fileName$fileExtension")

                saveBitmapToGallery(
                    exportedBitmap,
                    "exported_image_${System.currentTimeMillis()}",
                    currentExportOptions.format,
                    currentExportOptions.quality
                )


                var success = false
                try {
                    FileOutputStream(outputPath).use { out ->
                        exportedBitmap.compress(
                            currentExportOptions.format, // Use the selected format
                            currentExportOptions.quality, // Use the selected quality
                            out
                        )
                    }
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    exportedBitmap.recycle()
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            requireActivity(),
                            "Canvas exported successfully to ${outputPath.absolutePath} at ${currentExportOptions.resolution.name} with ${currentExportOptions.quality}% ${currentExportOptions.format}!",
                            Toast.LENGTH_LONG
                        ).show()
                        // You might want to show the saved image or offer to share it
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Failed to export canvas.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireActivity(),
                        "Failed to export canvas (bitmap null).",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _navController = null
        viewModel.clearCanvas()
        _binding = null
    }
}