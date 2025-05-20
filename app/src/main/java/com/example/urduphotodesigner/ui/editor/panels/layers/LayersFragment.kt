package com.example.urduphotodesigner.ui.editor.panels.layers

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentLayersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayersFragment : Fragment() {
    private var _binding: FragmentLayersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private lateinit var adapter: LayersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayersBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = LayersAdapter(
            onLockToggle = { element ->
                viewModel.setSelectedElement(element)
                viewModel.updateElement(element)
            },
            onRemove = { element ->
                viewModel.removeElement(element)
            },
            onItemClick = { element -> // Add this new callback
                viewModel.setSelectedElement(element) // Set the selected element in the ViewModel
            }
        )

        binding.layers.adapter = adapter

        // Setup drag and drop
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                // Get a mutable copy of the current elements from the ViewModel
                val currentElements = viewModel.canvasElements.value?.toMutableList() ?: return false

                if (fromPos < 0 || fromPos >= currentElements.size ||
                    toPos < 0 || toPos >= currentElements.size) {
                    return false
                }

                // Perform the reordering on the mutable list
                val movedElement = currentElements.removeAt(fromPos)
                currentElements.add(toPos, movedElement)

                // Update the ViewModel with the reordered list.
                // The ViewModel will then update zIndex based on the new positions and emit.
                viewModel.updateCanvasElementsOrderAndZIndex(currentElements)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used - we don't support swipe actions
            }

            override fun isLongPressDragEnabled(): Boolean = true
        })

        touchHelper.attachToRecyclerView(binding.layers)
    }

    private fun observeViewModel() {
        viewModel.canvasElements.observe(viewLifecycleOwner) { elements ->
            adapter.submitList(elements.sortedBy { it.zIndex })
            Log.d(TAG, "observeViewModel: ${elements[0].zIndex}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}