package com.webscare.urducanvas.viewmodels

/**
 * Names the searchable surfaces that keep a query in [MainViewModel].
 *
 * One constant per list that a query can filter. Two surfaces must never share a constant:
 * sharing is exactly the bug this replaced, where a single global query meant a term typed
 * in the Styles tab also filtered fonts, brushes and the home screen.
 *
 * The strings are internal keys, not analytics. `PanelSearchDialogFragment`'s `placement`
 * argument is a separate value with registered GA4 definitions behind it, and the two are
 * deliberately passed independently so a scope can be split without moving an event.
 */
object SearchScope {

    /** Text Properties → Styles. */
    const val TEXT_STYLES = "text_styles"

    /** Text Properties → Font. */
    const val TEXT_FONT = "text_font"

    /** Text Properties → 3D. Filters the Presets sub-page. */
    const val TEXT_3D = "text_3d"

    /** Text Properties → Symbols. Matches the glyph names, which the tiles do not print. */
    const val TEXT_SYMBOLS = "text_symbols"

    /** The text panel's own Fonts/Presets switcher, which is not inside the tab pager. */
    const val TEXT_PANEL = "text_panel"

    /** Draw → the brush catalogue. */
    const val DRAW_BRUSH = "draw_brush"

    /**
     * Table Properties → Font.
     *
     * The table panel reuses the text panel's `FontsFragment`, so without a scope of its own
     * its font list and the text panel's were literally the same query.
     */
    const val TABLE_FONT = "table_font"

    /** The home screen's own search screen. */
    const val HOME = "home"

    /** Every scope owned by the Text Properties tab pager, cleared when it closes. */
    val TEXT_ADJUSTMENTS_TABS = listOf(TEXT_STYLES, TEXT_FONT, TEXT_3D, TEXT_SYMBOLS)
}
