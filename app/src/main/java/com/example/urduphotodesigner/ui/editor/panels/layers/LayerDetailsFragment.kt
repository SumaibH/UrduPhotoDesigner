package com.example.urduphotodesigner.ui.editor.panels.layers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.databinding.FragmentLayerDetailsBinding
import com.example.urduphotodesigner.ui.editor.panels.text.fonts.FontsListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayerDetailsFragment : Fragment() {
    private var _binding: FragmentLayerDetailsBinding? = null
    private val binding get() = _binding!!
    private var currentLayer: String? = null
    private lateinit var selectedElements: ArrayList<CanvasElement>
    private val viewModel: CanvasViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLayer = arguments?.getString("currentLayer")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayerDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setEvents()
    }

    private fun setEvents() {
        binding.delete.setOnClickListener {
            viewModel.removeSelectedElements()
        }

        binding.lock.setOnClickListener {
            for (element in selectedElements){
                viewModel.updateElement(element)
            }
        }

        binding.visibility.setOnClickListener {
            for (element in selectedElements){
                viewModel.setOpacity(0)
            }
        }
    }

    private fun updateLockToggleDrawable() {
        if (selectedElements.isEmpty()) {
            // Optionally set a disabled or default icon
            binding.lock.setImageResource(R.drawable.ic_lock)
        } else {
            val allLocked = selectedElements.all { it.isLocked }
            val allUnlocked = selectedElements.all { !it.isLocked }
            // If mixed, you can choose an intermediate icon or choose one state; here, show locked if majority locked:
            when {
                allLocked -> binding.lock.setImageResource(R.drawable.ic_unlock)
                allUnlocked -> binding.lock.setImageResource(R.drawable.ic_lock)
                else -> {
                    // mixed: you could show a “partial” icon if you have one, or a filled icon to indicate that clicking will unlock?
                    // For simplicity, show filled lock to indicate some are locked:
                    binding.lock.setImageResource(R.drawable.ic_lock)
                }
            }
        }
    }

    private fun updateVisibilityToggleDrawable() {
        if (selectedElements.isEmpty()) {
            binding.visibility.setImageResource(R.drawable.ic_hide_pass)
        } else {
            val allVisible = selectedElements.all { it.paintAlpha == 255 }
            val allHidden = selectedElements.all { it.paintAlpha == 0 }
            when {
                allVisible -> binding.visibility.setImageResource(R.drawable.ic_hide_pass)
                allHidden -> binding.visibility.setImageResource(R.drawable.ic_show_pass)
                else -> {
                    // mixed: optional partial icon, or choose eye_open to indicate clicking will hide all
                    binding.visibility.setImageResource(R.drawable.ic_hide_pass)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.selectedElements.observe(viewLifecycleOwner) { elements ->
            selectedElements = elements as ArrayList<CanvasElement>
            updateVisibilityToggleDrawable()
            updateLockToggleDrawable()
            if (isAdded){
                if (elements.isNotEmpty()){
                    if (elements.size==1){
                        if (elements.first().type == ElementType.TEXT){
                            binding.layerName.setText(elements.first().text)
                        }else{
                            binding.layerName.setText("Sticker")
                        }
                    }else{
                        binding.layerName.setText("Mixed")
                    }
                }
            }
        }

        viewModel.opacity.observe(viewLifecycleOwner) { opacity ->
            if (isAdded){
                binding.opacitySize.text = "${opacity?.toInt() ?: 0}"
                binding.opacity.progress = opacity?.toInt() ?: 0
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_CURRENT_LAYER = "currentLayer"

        fun newInstance(fontCategory: String): FontsListFragment {
            val fragment = FontsListFragment()
            val args = Bundle()
            args.putString(ARG_CURRENT_LAYER, fontCategory)
            fragment.arguments = args
            return fragment
        }
    }
}