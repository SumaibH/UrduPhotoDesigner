package com.webscare.urducanvas.common.canvas.model

import com.google.gson.annotations.SerializedName
import com.webscare.urducanvas.common.canvas.enums.TableBorderMode
import com.webscare.urducanvas.common.canvas.enums.TableScope
import com.webscare.urducanvas.common.canvas.enums.TextAlignment
import com.webscare.urducanvas.common.canvas.enums.VAlign

data class TableTextStyle(
    @SerializedName("bgColor") var bgColor: Int? = null,
    @SerializedName("bgGradient") var bgGradient: GradientItem? = null,
    @SerializedName("textColor") var textColor: Int? = null,
    @SerializedName("textGradient") var textGradient: GradientItem? = null,
    @SerializedName("textSize") var textSize: Float? = null,
    @SerializedName("fontId") var fontId: String? = null,
    @SerializedName("isBold") var isBold: Boolean? = null,
    @SerializedName("isItalic") var isItalic: Boolean? = null,
    @SerializedName("isUnderline") var isUnderline: Boolean? = null,
    @SerializedName("hAlign") var hAlign: TextAlignment? = null,
    @SerializedName("vAlign") var vAlign: VAlign? = null,
    @SerializedName("lineSpacing") var lineSpacing: Float? = null,
    @SerializedName("letterSpacing") var letterSpacing: Float? = null,
    /**
     * True when this row's style is the alternating stripe a preset generated, rather than
     * something the user set on that row.
     *
     * The two have to be told apart because [TableData.rowStyles] is keyed by absolute row
     * index while the stripe pattern is defined relative to the body -- the preset writes a
     * stripe for every row between the header and the footer. Delete a row and those two
     * definitions disagree: a body stripe slides onto the new last row. Nothing in the
     * stored data says whether the entry sitting there was meant for a body row or put
     * there deliberately, so the origin has to be recorded when it is written.
     */
    @SerializedName("autoStripe") var autoStripe: Boolean = false
) {
    fun deepCopy(): TableTextStyle = copy(
        bgGradient = bgGradient?.copy(),
        textGradient = textGradient?.copy()
    )
}

data class TableCell(
    @SerializedName("text") var text: String = "",
    @SerializedName("override") var override: TableTextStyle? = null
) {
    fun deepCopy(): TableCell = TableCell(
        text = text,
        override = override?.deepCopy()
    )
}

data class TableData(
    @SerializedName("rows") var rows: Int = 3,
    @SerializedName("cols") var cols: Int = 3,
    @SerializedName("borderWidth") var borderWidth: Float = 2f,
    @SerializedName("borderColor") var borderColor: Int = android.graphics.Color.BLACK,
    @SerializedName("borderGradient") var borderGradient: GradientItem? = null,
    @SerializedName("cornerRadius") var cornerRadius: Float = 0f,
    @SerializedName("borderMode") var borderMode: TableBorderMode = TableBorderMode.ALL,
    @SerializedName("paddingH") var paddingH: Float = 8f,
    @SerializedName("paddingV") var paddingV: Float = 8f,
    @SerializedName("hasHeader") var hasHeader: Boolean = true,
    @SerializedName("hasFooter") var hasFooter: Boolean = false,
    @SerializedName("hasHeaderCol") var hasHeaderCol: Boolean = false,
    @SerializedName("isRTL") var isRTL: Boolean = true,
    @SerializedName("contentWrap") var contentWrap: Boolean = false,
    @SerializedName("base") var base: TableTextStyle = TableTextStyle(),
    @SerializedName("headerStyle") var headerStyle: TableTextStyle = TableTextStyle(),
    @SerializedName("footerStyle") var footerStyle: TableTextStyle = TableTextStyle(),
    @SerializedName("headerColStyle") var headerColStyle: TableTextStyle = TableTextStyle(),
    @SerializedName("rowStyles") var rowStyles: MutableMap<Int, TableTextStyle> = mutableMapOf(),
    @SerializedName("colStyles") var colStyles: MutableMap<Int, TableTextStyle> = mutableMapOf(),
    @SerializedName("colWidthRatios") var colWidthRatios: MutableList<Float>? = null,
    @SerializedName("rowHeightRatios") var rowHeightRatios: MutableList<Float>? = null,
    @SerializedName("cells") var cells: MutableList<MutableList<TableCell>> = mutableListOf(),
    // Which cells the user currently has picked. Editor state, not document state: it used
    // to be written into the saved design, so reopening a file came back with cells still
    // highlighted from whenever it was last saved.
    @field:Transient var selectedCells: MutableSet<Pair<Int, Int>> = mutableSetOf()
) {

    /**
     * Keeps the index-keyed state in step after a row is inserted at [index].
     *
     * Call once [rows] and [cells] have been updated. Track sizes, per-row styles and the
     * cell selection are all keyed by absolute row index, so an insert anywhere but the end
     * moves every one of them -- which is why these take a position instead of just
     * resizing to the new count. "+ Row Above" inserts at 0, so getting this wrong shifts
     * the whole table's styling down by one row.
     */
    fun onRowInserted(index: Int) {
        rowHeightRatios = rowHeightRatios?.let { insertTrack(it, index, rows) }
        rowStyles = shiftKeysForInsert(rowStyles, index)
        selectedCells = selectedCells.mapTo(mutableSetOf()) { (r, c) ->
            if (r >= index) (r + 1) to c else r to c
        }
        pruneToGrid()
    }

    /** Counterpart to [onRowInserted], for the row that used to sit at [index]. */
    fun onRowRemoved(index: Int) {
        rowHeightRatios = rowHeightRatios?.let { removeTrack(it, index, rows) }
        rowStyles = shiftKeysForRemove(rowStyles, index)
        selectedCells = selectedCells.mapNotNullTo(mutableSetOf()) { (r, c) ->
            when {
                r == index -> null
                r > index -> (r - 1) to c
                else -> r to c
            }
        }
        pruneToGrid()
    }

    /** As [onRowInserted], for a column. Note [index] is logical, not left-to-right. */
    fun onColumnInserted(index: Int) {
        colWidthRatios = colWidthRatios?.let { insertTrack(it, index, cols) }
        colStyles = shiftKeysForInsert(colStyles, index)
        selectedCells = selectedCells.mapTo(mutableSetOf()) { (r, c) ->
            if (c >= index) r to (c + 1) else r to c
        }
        pruneToGrid()
    }

    /** As [onRowRemoved], for a column. */
    fun onColumnRemoved(index: Int) {
        colWidthRatios = colWidthRatios?.let { removeTrack(it, index, cols) }
        colStyles = shiftKeysForRemove(colStyles, index)
        selectedCells = selectedCells.mapNotNullTo(mutableSetOf()) { (r, c) ->
            when {
                c == index -> null
                c > index -> r to (c - 1)
                else -> r to c
            }
        }
        pruneToGrid()
    }

    /**
     * A preset brings its own geometry and its own styling, so anything keyed to the old
     * grid is gone. Dragged track sizes are kept only when they still fit, since the layout
     * builder discards a ratio list whose length does not match and would leave the stale
     * one behind for good.
     */
    fun onGridReplaced() {
        if (colWidthRatios?.size != cols) colWidthRatios = null
        if (rowHeightRatios?.size != rows) rowHeightRatios = null
        pruneToGrid()
    }

    /** Drops anything still pointing outside the current grid. */
    private fun pruneToGrid() {
        rowStyles.keys.retainAll { it in 0 until rows }
        colStyles.keys.retainAll { it in 0 until cols }
        selectedCells.retainAll { it.first in 0 until rows && it.second in 0 until cols }
    }

    private fun insertTrack(src: MutableList<Float>, index: Int, target: Int): MutableList<Float> {
        if (target <= 0) return src
        val out = src.toMutableList()
        out.add(index.coerceIn(0, out.size), 1f / target)
        return normaliseTracks(out, target)
    }

    private fun removeTrack(src: MutableList<Float>, index: Int, target: Int): MutableList<Float> {
        if (target <= 0) return src
        val out = src.toMutableList()
        if (index in out.indices) out.removeAt(index)
        return normaliseTracks(out, target)
    }

    private fun normaliseTracks(out: MutableList<Float>, target: Int): MutableList<Float> {
        while (out.size > target) out.removeAt(out.size - 1)
        while (out.size < target) out.add(1f / target)
        val sum = out.sum()
        return if (sum > 0f) out.map { it / sum }.toMutableList()
        else MutableList(target) { 1f / target }
    }

    private fun shiftKeysForInsert(src: MutableMap<Int, TableTextStyle>, index: Int) =
        src.mapKeys { (k, _) -> if (k >= index) k + 1 else k }.toMutableMap()

    private fun shiftKeysForRemove(src: MutableMap<Int, TableTextStyle>, index: Int) =
        src.filterKeys { it != index }
            .mapKeys { (k, _) -> if (k > index) k - 1 else k }
            .toMutableMap()
    fun deepCopy(): TableData {
        return TableData(
            rows = rows,
            cols = cols,
            borderWidth = borderWidth,
            borderColor = borderColor,
            borderGradient = borderGradient?.copy(),
            cornerRadius = cornerRadius,
            borderMode = borderMode,
            paddingH = paddingH,
            paddingV = paddingV,
            hasHeader = hasHeader,
            hasFooter = hasFooter,
            hasHeaderCol = hasHeaderCol,
            isRTL = isRTL,
            contentWrap = contentWrap,
            base = base.deepCopy(),
            headerStyle = headerStyle.deepCopy(),
            footerStyle = footerStyle.deepCopy(),
            headerColStyle = headerColStyle.deepCopy(),
            rowStyles = rowStyles.mapValues { it.value.deepCopy() }.toMutableMap(),
            colStyles = colStyles.mapValues { it.value.deepCopy() }.toMutableMap(),
            colWidthRatios = colWidthRatios?.toMutableList(),
            rowHeightRatios = rowHeightRatios?.toMutableList(),
            cells = cells.map { row -> row.map { cell -> cell.deepCopy() }.toMutableList() }.toMutableList(),
            selectedCells = selectedCells.toMutableSet()
        )
    }

    fun allFontIds(): List<String> {
        val list = mutableListOf<String>()
        base.fontId?.let { list.add(it) }
        headerStyle.fontId?.let { list.add(it) }
        footerStyle.fontId?.let { list.add(it) }
        headerColStyle.fontId?.let { list.add(it) }
        rowStyles.values.forEach { s -> s.fontId?.let { list.add(it) } }
        colStyles.values.forEach { s -> s.fontId?.let { list.add(it) } }
        cells.forEach { row ->
            row.forEach { cell ->
                cell.override?.fontId?.let { list.add(it) }
            }
        }
        return list.distinct()
    }

    fun applyFontToScope(fontId: String, scope: TableScope = TableScope.WHOLE_TABLE, row: Int = 0, col: Int = 0) {
        if (selectedCells.isNotEmpty()) {
            for (cellPair in selectedCells) {
                val r = cellPair.first
                val c = cellPair.second
                if (r in 0 until rows && c in 0 until cols) {
                    val cell = cells[r][c]
                    val cellOverride = cell.override ?: TableTextStyle().also { cell.override = it }
                    cellOverride.fontId = fontId
                }
            }
            return
        }
        // When no cells are selected, apply font to the entire table
        base.fontId = fontId
        headerStyle.fontId = fontId
        footerStyle.fontId = fontId
        headerColStyle.fontId = fontId
        rowStyles.values.forEach { it.fontId = fontId }
        colStyles.values.forEach { it.fontId = fontId }
        cells.forEach { rowList ->
            rowList.forEach { cell ->
                cell.override?.fontId = fontId
            }
        }
    }


    companion object {
        fun createDefault(r: Int = 3, c: Int = 3, withHeader: Boolean = true): TableData {
            val validR = r.coerceAtLeast(1)
            val validC = c.coerceAtLeast(1)
            val data = TableData(rows = validR, cols = validC, hasHeader = withHeader, paddingH = 10f, paddingV = 8f)
            data.base = TableTextStyle(hAlign = TextAlignment.RIGHT, vAlign = VAlign.MIDDLE)
            data.cells = MutableList(validR) { MutableList(validC) { TableCell() } }
            if (withHeader && validR > 0) {
                data.headerStyle = TableTextStyle(
                    isBold = true,
                    hAlign = TextAlignment.RIGHT,
                    vAlign = VAlign.MIDDLE,
                    bgColor = android.graphics.Color.parseColor("#E4F3E9")
                )
                val urduDigits = listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۱۰")
                for (colIdx in 0 until validC) {
                    val num = if (colIdx < urduDigits.size) urduDigits[colIdx] else "${colIdx + 1}"
                    data.cells[0][colIdx].text = "عنوان $num"
                }
            }
            return data
        }
    }
}
