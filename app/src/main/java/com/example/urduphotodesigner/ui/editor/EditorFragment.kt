package com.example.urduphotodesigner.ui.editor

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.Converter.cmToPx
import com.example.urduphotodesigner.common.Converter.inchesToPx
import com.example.urduphotodesigner.common.enums.UnitType
import com.example.urduphotodesigner.common.views.SizedCanvasView
import com.example.urduphotodesigner.data.model.CanvasSize
import com.example.urduphotodesigner.databinding.ActivityMainBinding
import com.example.urduphotodesigner.databinding.FragmentCreateBinding
import com.example.urduphotodesigner.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorFragment : Fragment() {
    private var _binding: FragmentEditorBinding?= null
    private val binding get() = _binding!!

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private lateinit var canvasSize: CanvasSize
    private var currentUnit = UnitType.PIXELS

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
    }

    private fun setEvents() {
        // Setup navigation
        val navHostFragment = childFragmentManager.findFragmentById(R.id.panelNavHost) as NavHostFragment
        _navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_background -> navController.navigate(R.id.backgroundsFragment)
                R.id.nav_objects -> navController.navigate(R.id.objectsFragment)
                R.id.nav_text -> navController.navigate(R.id.textFragment)
                R.id.nav_images -> navController.navigate(R.id.imagesFragment)
                else -> false
            }
            true
        }

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
        Log.d(TAG, "setEvents: $widthPx x $heightPx")
        // Create and add canvas
        val canvasView = SizedCanvasView(
            requireContext(),
            canvasWidth = widthPx,
            canvasHeight = heightPx
        )

        binding.canvasContainer.removeAllViews()
        binding.canvasContainer.addView(canvasView)
    }

    override fun onDestroy() {
        super.onDestroy()
        _navController = null
        _binding = null
    }
}