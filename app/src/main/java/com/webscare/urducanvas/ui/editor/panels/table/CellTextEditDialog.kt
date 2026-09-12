package com.webscare.urducanvas.ui.editor.panels.table

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.model.TableData
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.DialogCanvasCellEditBinding

class CellTextEditDialog : DialogFragment() {

    private var _binding: DialogCanvasCellEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()

    private var currentRow = 0
    private var currentCol = 0

    /**
     * How far the cell index moves when the left-hand and right-hand halves of the pager
     * are tapped.
     *
     * Columns are drawn mirrored in an RTL table ([com.webscare.urducanvas.common.canvas.cache.TableLayoutCache]
     * maps logical column `c` to visual `cols - 1 - c`), so stepping +1 there walks the
     * selection to the *left* of the screen. Binding the deltas to the table's direction
     * keeps each half of the pager pointing the way the highlight will actually travel;
     * before this, both arrows pointed the wrong way for every Urdu table.
     */
    private var startDelta = -1
    private var endDelta = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // A light scrim lifts the card off the canvas without hiding the table —
            // the cell being edited is highlighted up there and needs to stay readable.
            setDimAmount(0.2f)
            setGravity(android.view.Gravity.BOTTOM)
            setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCanvasCellEditBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentRow = viewModel.selectedTableRow.value
        currentCol = viewModel.selectedTableCol.value

        loadCellData(currentRow, currentCol)

        binding.editCellInput.requestFocus()
        binding.editCellInput.doAfterTextChanged { text ->
            binding.btnClearCell.isVisible = !text.isNullOrEmpty()
        }

        // The field stays multi-line on purpose. In Auto Expand a column is measured as the
        // widest of a cell's newline-separated lines and nothing else wraps it
        // (TableLayoutCache.build), so a typed break is the only way to stop one long cell
        // forcing a very wide column. That rules out trading it for a keyboard "Next" key,
        // so stepping lives on the pager below instead. This listener still honours an
        // action key on the keyboards that offer one alongside Enter.
        binding.editCellInput.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    saveCurrentCellText()
                    advanceCell(1)
                    true
                }

                EditorInfo.IME_ACTION_DONE -> {
                    saveCurrentCellText()
                    dismiss()
                    true
                }

                else -> false
            }
        }

        binding.btnClearCell.addPressEffect {
            binding.editCellInput.setText("")
            binding.editCellInput.requestFocus()
        }

        binding.btnDone.addPressEffect {
            saveCurrentCellText()
            dismiss()
        }

        binding.btnStepEnd.addPressEffect {
            saveCurrentCellText()
            advanceCell(endDelta)
        }

        binding.btnStepStart.addPressEffect {
            saveCurrentCellText()
            advanceCell(startDelta)
        }
    }

    /**
     * The live table data.
     *
     * This must come off the canvas list rather than off `selectedElements`: editing a cell
     * replaces the element in `_canvasElements` and leaves `selectedElements` pointing at the
     * element as it was before the edit. Reading that stale copy meant stepping Next and then
     * Prev showed the cell's *old* text, and Done then wrote that old text back over the edit.
     */
    private fun getSelectedTableData(): TableData? = viewModel.getSelectedTableData()

    private fun loadCellData(row: Int, col: Int) {
        val tableData = getSelectedTableData() ?: return
        if (row !in 0 until tableData.rows || col !in 0 until tableData.cols) return

        currentRow = row
        currentCol = col

        // Move the highlight on the canvas with the sheet. Stepping used to update only the
        // scope, so the green outline stayed on whatever cell was tapped first and the sheet
        // gave no clue which cell the text belonged to. Selection is mutated in place rather
        // than through updateSelectedTableData, which would push an undo entry per step.
        viewModel.setSelectedTableCells(setOf(row to col))
        viewModel.setTableScope(com.webscare.urducanvas.common.canvas.enums.TableScope.CELL, row, col)
        viewModel.getCanvasView()?.invalidate()

        bindContext(tableData, row, col)
        bindPager(tableData, row, col)

        val currentText = tableData.cells.getOrNull(row)?.getOrNull(col)?.text.orEmpty()
        binding.editCellInput.setText(currentText)
        binding.editCellInput.setSelection(currentText.length)
        binding.btnClearCell.isVisible = currentText.isNotEmpty()
    }

    /**
     * Names the cell the way the person filling it in thinks of it.
     *
     * A table with a header row already says what each column holds, so the column's own
     * heading is a far better label than a grid coordinate — "Row 3 / اسم" tells you what
     * to type, where "row 3 · col 2" only tells you where you are.
     */
    private fun bindContext(tableData: TableData, row: Int, col: Int) {
        val headerText = if (tableData.hasHeader) {
            tableData.cells.getOrNull(0)?.getOrNull(col)?.text?.trim().orEmpty()
        } else ""

        val editingHeader = tableData.hasHeader && row == 0

        when {
            editingHeader -> {
                binding.tvCellPosition.text = "Column ${col + 1}"
                binding.tvHeaderName.text = "Column heading"
            }

            headerText.isNotEmpty() -> {
                binding.tvCellPosition.text = "Row ${row + 1} of ${tableData.rows}"
                binding.tvHeaderName.text = headerText
            }

            else -> {
                binding.tvCellPosition.text = "Row ${row + 1} of ${tableData.rows}"
                binding.tvHeaderName.text = "Column ${col + 1}"
            }
        }
    }

    private fun bindPager(tableData: TableData, row: Int, col: Int) {
        // Left half always walks the selection left, whichever direction the table runs.
        startDelta = if (tableData.isRTL) 1 else -1
        endDelta = -startDelta

        binding.btnStepStart.text = if (tableData.isRTL) "Next" else "Previous"
        binding.btnStepEnd.text = if (tableData.isRTL) "Previous" else "Next"

        val totalCells = tableData.rows * tableData.cols
        val currentIndex = row * tableData.cols + col

        binding.tvCellCounter.text = "${currentIndex + 1} of $totalCells"

        setStepEnabled(binding.btnStepStart, currentIndex + startDelta in 0 until totalCells)
        setStepEnabled(binding.btnStepEnd, currentIndex + endDelta in 0 until totalCells)
    }

    private fun setStepEnabled(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.38f
    }

    private fun saveCurrentCellText() {
        val text = binding.editCellInput.text?.toString().orEmpty()
        viewModel.setTableCellText(currentRow, currentCol, text)
    }

    private fun advanceCell(delta: Int) {
        val tableData = getSelectedTableData() ?: return
        val totalCells = tableData.rows * tableData.cols
        if (totalCells <= 0) return

        val currentIndex = currentRow * tableData.cols + currentCol
        val nextIndex = (currentIndex + delta).coerceIn(0, totalCells - 1)

        val nextRow = nextIndex / tableData.cols
        val nextCol = nextIndex % tableData.cols

        loadCellData(nextRow, nextCol)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CellTextEditDialog()
    }
}
