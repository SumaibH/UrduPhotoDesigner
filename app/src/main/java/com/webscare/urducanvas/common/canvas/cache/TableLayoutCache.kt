package com.webscare.urducanvas.common.canvas.cache

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.webscare.urducanvas.common.canvas.enums.TextAlignment
import com.webscare.urducanvas.common.canvas.enums.VAlign
import com.webscare.urducanvas.common.canvas.model.TableData
import com.webscare.urducanvas.common.canvas.model.TableTextStyle

data class CellLayout(
    val rect: RectF,
    val style: TableTextStyle,
    val lines: List<String>,
    val paint: TextPaint
)

class TableLayoutCache(
    val width: Float,
    val height: Float,
    val rows: Int,
    val cols: Int,
    val rowHeightsPx: List<Float>,
    val colWidthsPx: List<Float>,
    val cellLayouts: List<List<CellLayout>>,
    /**
     * The vertical grid lines, left to right: [cols] + 1 offsets from the table's centre
     * origin. Published because the borders have to fall exactly where the cells do, and
     * [colWidthsPx] is in logical order -- summing it works out to the same offsets only
     * while the table is LTR or every column is the same width.
     */
    val colEdgesPx: List<Float>,
    /** The horizontal grid lines, top to bottom: [rows] + 1 offsets. */
    val rowEdgesPx: List<Float>
) {
    companion object {
        fun build(
            data: TableData,
            totalW: Float,
            totalH: Float,
            fontLookup: (String?) -> Typeface?
        ): TableLayoutCache {
            val rCount = data.rows.coerceAtLeast(1)
            val cCount = data.cols.coerceAtLeast(1)

            val colRatios = data.colWidthRatios?.takeIf { it.size == cCount }
            val colSum = colRatios?.sum()?.takeIf { it > 0f } ?: cCount.toFloat()
            val colWidths = if (colRatios != null) {
                colRatios.map { (it / colSum) * totalW }
            } else if (data.contentWrap) {
                // Auto Expand: measure content width of each column
                val autoWidths = (0 until cCount).map { c ->
                    var maxW = 40f
                    for (r in 0 until rCount) {
                        val cellObj = if (r < data.cells.size && c < data.cells[r].size) data.cells[r][c] else null
                        val mergedStyle = mergeStyle(data, r, c, cellObj?.override)
                        val text = cellObj?.text ?: ""
                        if (text.isNotBlank()) {
                            val tf = fontLookup(mergedStyle.fontId)
                            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = mergedStyle.textSize ?: 16f
                                if (mergedStyle.letterSpacing != null) {
                                    letterSpacing = (mergedStyle.letterSpacing ?: 0f) / 50f
                                }
                                val styleInt = when {
                                    mergedStyle.isBold == true && mergedStyle.isItalic == true -> Typeface.BOLD_ITALIC
                                    mergedStyle.isBold == true -> Typeface.BOLD
                                    mergedStyle.isItalic == true -> Typeface.ITALIC
                                    else -> Typeface.NORMAL
                                }
                                typeface = if (tf != null) Typeface.create(tf, styleInt) else Typeface.create(Typeface.DEFAULT, styleInt)
                            }
                            for (line in text.split("\n")) {
                                val w = paint.measureText(line) + (data.paddingH * 2) + 16f
                                if (w > maxW) maxW = w
                            }
                        }
                    }
                    maxW
                }
                val autoSum = autoWidths.sum().coerceAtLeast(100f)
                autoWidths.map { (it / autoSum) * totalW }
            } else {
                List(cCount) { totalW / cCount }
            }

            val rowRatios = data.rowHeightRatios?.takeIf { it.size == rCount }
            val rowSum = rowRatios?.sum()?.takeIf { it > 0f } ?: rCount.toFloat()
            val rowHeights = if (rowRatios != null) {
                rowRatios.map { (it / rowSum) * totalH }
            } else {
                List(rCount) { totalH / rCount }
            }

            val left0 = -totalW / 2f
            val top0 = -totalH / 2f

            // Left-to-right placement order. In an RTL table the first logical column is the
            // rightmost one on screen, so the offsets have to accumulate over the *reversed*
            // widths. Accumulating over the logical order and then reading the slot at the
            // mirrored index gave every column a different column's width -- which is why
            // "Auto Expand" handed the single-digit index column the width measured for the
            // price column, and squeezed the prices into the index column's width.
            val visualWidths = if (data.isRTL) colWidths.reversed() else colWidths
            val visualLefts = mutableListOf<Float>()
            var curX = left0
            visualWidths.forEach { w ->
                visualLefts.add(curX)
                curX += w
            }

            val rowTops = mutableListOf<Float>()
            var curY = top0
            rowHeights.forEach { h ->
                rowTops.add(curY)
                curY += h
            }

            val layouts = List(rCount) { r ->
                val rTop = rowTops[r]
                val rBottom = rTop + rowHeights[r]
                List(cCount) { c ->
                    // Where this column sits on screen, carrying its own width with it.
                    val visualCol = if (data.isRTL) (cCount - 1 - c) else c
                    val cWidth = colWidths[c]
                    val cLeft = visualLefts[visualCol]
                    val cRight = cLeft + cWidth
                    val cellRect = RectF(cLeft, rTop, cRight, rBottom)
                    val cellObj = if (r < data.cells.size && c < data.cells[r].size) data.cells[r][c] else null
                    val mergedStyle = mergeStyle(data, r, c, cellObj?.override)
                    val text = cellObj?.text ?: ""
                    val tf = fontLookup(mergedStyle.fontId)
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = mergedStyle.textColor ?: android.graphics.Color.BLACK
                        textSize = mergedStyle.textSize ?: 16f
                        if (mergedStyle.letterSpacing != null) {
                            letterSpacing = (mergedStyle.letterSpacing ?: 0f) / 50f
                        }
                        isUnderlineText = mergedStyle.isUnderline == true
                        val styleInt = when {
                            mergedStyle.isBold == true && mergedStyle.isItalic == true -> Typeface.BOLD_ITALIC
                            mergedStyle.isBold == true -> Typeface.BOLD
                            mergedStyle.isItalic == true -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        typeface = if (tf != null) Typeface.create(tf, styleInt) else Typeface.create(Typeface.DEFAULT, styleInt)
                    }
                    val availableTextWidth = (cWidth - (data.paddingH * 2)).coerceAtLeast(10f)
                    val lines = if (data.contentWrap) {
                        if (text.isNotBlank()) text.split("\n") else emptyList()
                    } else {
                        wrapTextToLines(text, paint, availableTextWidth)
                    }
                    CellLayout(cellRect, mergedStyle, lines, paint)
                }
            }

            val colEdges = visualLefts + (left0 + visualWidths.sum())
            val rowEdges = rowTops + (top0 + rowHeights.sum())

            return TableLayoutCache(
                totalW, totalH, rCount, cCount, rowHeights, colWidths, layouts, colEdges, rowEdges
            )
        }

        private fun wrapTextToLines(text: String, paint: TextPaint, maxAvailableWidth: Float): List<String> {
            if (text.isBlank()) return emptyList()
            if (maxAvailableWidth <= 0f) return text.split("\n")
            val result = mutableListOf<String>()
            val rawParagraphs = text.split("\n")
            for (paragraph in rawParagraphs) {
                if (paragraph.isBlank()) {
                    result.add("")
                    continue
                }
                val words = paragraph.split(" ")
                var currentLine = StringBuilder()
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) <= maxAvailableWidth) {
                        currentLine = StringBuilder(testLine)
                    } else {
                        if (currentLine.isNotEmpty()) {
                            result.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        } else {
                            result.add(word)
                        }
                    }
                }
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                }
            }
            return result
        }

        private fun mergeStyle(data: TableData, r: Int, c: Int, override: TableTextStyle?): TableTextStyle {
            val res = TableTextStyle()
            fun applyStyle(s: TableTextStyle?) {
                if (s == null) return
                s.bgColor?.let { res.bgColor = it }
                s.bgGradient?.let { res.bgGradient = it }
                s.textColor?.let { res.textColor = it }
                s.textGradient?.let { res.textGradient = it }
                s.textSize?.let { res.textSize = it }
                s.fontId?.let { res.fontId = it }
                s.isBold?.let { res.isBold = it }
                s.isItalic?.let { res.isItalic = it }
                s.isUnderline?.let { res.isUnderline = it }
                s.hAlign?.let { res.hAlign = it }
                s.vAlign?.let { res.vAlign = it }
                s.lineSpacing?.let { res.lineSpacing = it }
                s.letterSpacing?.let { res.letterSpacing = it }
            }
            val isHeaderRow = data.hasHeader && r == 0
            val isFooterRow = data.hasFooter && r == data.rows - 1 && data.rows > 1

            applyStyle(data.base)
            if (isHeaderRow) applyStyle(data.headerStyle)
            if (isFooterRow) applyStyle(data.footerStyle)
            if (data.hasHeaderCol && c == 0) applyStyle(data.headerColStyle)
            // Row styles are applied after the header and footer because a style set on one
            // specific row is the more specific instruction of the two -- but a generated
            // stripe is not. Deleting a row slid a body stripe onto the new last row, where
            // it overrode footerStyle and the footer stopped looking like a footer. A stripe
            // yields to those roles; a row the user styled themselves still wins.
            data.rowStyles[r]?.let { rowStyle ->
                if (!(rowStyle.autoStripe && (isHeaderRow || isFooterRow))) applyStyle(rowStyle)
            }
            applyStyle(data.colStyles[c])
            applyStyle(override)
            return res
        }
    }
}
