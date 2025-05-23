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
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
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
                val currentElements = viewModel.canvasElements.value?.toMutableList() ?: mutableListOf()

                // Find the actual element object from the ViewModel's list to modify its isSelected state
                val elementToToggle = currentElements.find { it.id == element.id }

                elementToToggle?.let {
                    // Toggle its selection state
                    it.isSelected = !it.isSelected

                    // If the clicked element is now selected, and it's the only one selected,
                    // or if it was previously the only one selected and we're deselecting it,
                    // we need to ensure other elements are deselected if not explicitly multi-selecting.
                    // However, the requirement is that multi-selection is *only* from LayersFragment.
                    // So, if an item is clicked, its selection state is simply toggled.
                    // The SizedCanvasView will handle single-selection on canvas interaction.

                    // Collect all currently selected elements after the toggle
                    val selectedElements = currentElements.filter { it.isSelected }

                    // Inform the ViewModel about the new selection state of all elements
                    viewModel.setSelectedElementsFromLayers(selectedElements)
                }
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

                val currentElements = viewModel.canvasElements.value?.toMutableList() ?: return false

                if (fromPos < 0 || fromPos >= currentElements.size ||
                    toPos < 0 || toPos >= currentElements.size) {
                    return false
                }

                val draggedElement = currentElements[fromPos]

                // Get all currently selected elements, sorted by their original zIndex (which corresponds to list order)
                val selectedElementsInOrder = currentElements.filter { it.isSelected }.sortedBy { currentElements.indexOf(it) }

                // If the dragged element is part of the current selection, move the entire block
                if (selectedElementsInOrder.contains(draggedElement)) {
                    val finalReorderedList = mutableListOf<CanvasElement>()
                    val nonSelectedElements = currentElements.filter { !it.isSelected }.toMutableList()

                    // Calculate the effective target position for the *start* of the selected block.
                    // This is based on where the dragged element is moved to,
                    // relative to its position within the selected block.
                    val indexOfDraggedInSelected = selectedElementsInOrder.indexOf(draggedElement)
                    var blockTargetStartPos = toPos - indexOfDraggedInSelected

                    // Clamp the blockTargetStartPos to valid range within the final list size
                    blockTargetStartPos = blockTargetStartPos.coerceIn(0, currentElements.size - selectedElementsInOrder.size)

                    // Build the new list by inserting selected elements at their calculated block start position
                    var selectedIndex = 0
                    var nonSelectedIndex = 0

                    for (i in 0 until currentElements.size) {
                        if (i >= blockTargetStartPos && selectedIndex < selectedElementsInOrder.size) {
                            // Insert selected elements at the calculated block start position
                            finalReorderedList.add(selectedElementsInOrder[selectedIndex])
                            selectedIndex++
                        } else {
                            // Insert non-selected elements, ensuring we don't re-add selected ones
                            if (nonSelectedIndex < nonSelectedElements.size) {
                                finalReorderedList.add(nonSelectedElements[nonSelectedIndex])
                                nonSelectedIndex++
                            }
                        }
                    }
                    // Add any remaining selected elements if they were supposed to go past the end
                    while (selectedIndex < selectedElementsInOrder.size) {
                        finalReorderedList.add(selectedElementsInOrder[selectedIndex])
                        selectedIndex++
                    }

                    viewModel.updateCanvasElementsOrderAndZIndex(finalReorderedList)
                    return true
                } else {
                    // If the dragged element is not selected, perform single element reordering
                    val movedElement = currentElements.removeAt(fromPos)
                    currentElements.add(toPos, movedElement)
                    viewModel.updateCanvasElementsOrderAndZIndex(currentElements)
                    return true
                }
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}