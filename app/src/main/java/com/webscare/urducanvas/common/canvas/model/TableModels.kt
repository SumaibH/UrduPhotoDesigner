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
     * Brings the stored column/row tracks back in step with [cols] and [rows].
     *
     * The layout builder ignores a ratio list whose length does not match the grid, so
     * adding or deleting a row or column used to silently throw away every width the user
     * had dragged, and leave a wrong-length list behind for good. A new track gets an even
     * share, a removed one is dropped from the tail, and the list is renormalised either
     * way. Any selection that now points outside the grid is dropped at the same time.
     */
    fun onGridResized() {
        colWidthRatios = colWidthRatios?.let { fitTracks(it, cols) }
        rowHeightRatios = rowHeightRatios?.let { fitTracks(it, rows) }
        selectedCells.retainAll { it.first in 0 until rows && it.second in 0 until cols }
        // Styles are keyed by absolute index, so shrinking the grid strands the entries for
        // the rows and columns that are gone. They are not harmless: grow the table again and
        // the new, blank row comes back wearing the deleted row's styling.
        rowStyles.keys.retainAll { it in 0 until rows }
        colStyles.keys.retainAll { it in 0 until cols }
    }

    private fun fitTracks(src: MutableList<Float>, target: Int): MutableList<Float> {
        if (target <= 0) return src
        val out = src.toMutableList()
        while (out.size > target) out.removeAt(out.size - 1)
        while (out.size < target) out.add(1f / target)
        val sum = out.sum()
        return if (sum > 0f) out.map { it / sum }.toMutableList()
        else MutableList(target) { 1f / target }
    }
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
