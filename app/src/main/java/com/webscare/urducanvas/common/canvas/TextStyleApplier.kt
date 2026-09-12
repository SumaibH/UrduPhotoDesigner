package com.webscare.urducanvas.common.canvas

import android.graphics.Color
import com.webscare.urducanvas.common.canvas.model.CanvasElement
import com.webscare.urducanvas.data.model.TextStylePreset

/**
 * Writes a [TextStylePreset]'s fifty-odd styling fields onto a [CanvasElement].
 *
 * This mapping existed twice — once in `CanvasViewModel.applyTextStylePreset`, which
 * repaints the current selection, and once in `addTextWithStyle`, which builds a new
 * element. The two had already drifted: one let a style with no colour of its own keep
 * the element's fill, the other forced black. A preset is a lockup of several layers,
 * each wearing a style by id, so a third copy was about to appear — and a preset that
 * renders differently from the style it names would undo the whole reason presets point
 * at styles instead of carrying their own paint.
 *
 * So there is one copy, and everything calls it.
 */
object TextStyleApplier {

    /**
     * Returns a copy of [element] wearing [style].
     *
     * A style with no fill colour of its own leaves the element's fill alone rather than
     * forcing a default — that is what makes the synthetic "None" style a reset for every
     * effect without also repainting text the user coloured by hand. Build a fresh element
     * with the fill you want as its default and this rule gives the same answer.
     */
    fun apply(element: CanvasElement, style: TextStylePreset): CanvasElement {
        // Derived rather than stored: the catalogue records a stroke colour and a width,
        // and a width of zero means no stroke however the colour reads. Same for shadow,
        // which is on if it has any radius or any offset.
        val hasStrokeVal = style.strokeColor != null && style.strokeWidth > 0f
        val hasShadowVal = style.shadowColor != null &&
                (style.shadowRadius > 0f || style.shadowDx != 0f || style.shadowDy != 0f)

        return element.copy(
            paintColor = style.textColor ?: element.paintColor,
            fillGradient = style.textGradient,
            hasStroke = hasStrokeVal,
            strokeColor = style.strokeColor ?: Color.TRANSPARENT,
            strokeWidth = style.strokeWidth,
            hasUnderStroke = style.hasUnderStroke,
            underStrokeColor = style.underStrokeColor ?: Color.TRANSPARENT,
            underStrokeWidth = style.underStrokeWidth,
            has3dExtrude = style.has3dExtrude,
            extrudeColor = style.extrudeColor ?: Color.BLACK,
            extrudeDepth = style.extrudeDepth,
            extrudeDx = style.extrudeDx,
            extrudeDy = style.extrudeDy,
            hasDoubleExtrude = style.hasDoubleExtrude,
            extrudeStep2Color = style.extrudeStep2Color ?: Color.BLACK,
            extrudeStep2Depth = style.extrudeStep2Depth,
            extrudeStep2Dx = style.extrudeStep2Dx,
            extrudeStep2Dy = style.extrudeStep2Dy,
            hasAnaglyph = style.hasAnaglyph,
            anaglyphOffset = style.anaglyphOffset,
            anaglyphColor1 = style.anaglyphColor1 ?: DEFAULT_ANAGLYPH_1,
            anaglyphColor2 = style.anaglyphColor2 ?: DEFAULT_ANAGLYPH_2,
            hasBevel = style.hasBevel,
            bevelHighlightColor = style.bevelHighlightColor ?: DEFAULT_HIGHLIGHT,
            bevelShadowColor = style.bevelShadowColor ?: DEFAULT_SHADOW,
            bevelDepth = style.bevelDepth,
            hasEmboss = style.hasEmboss,
            isDebossed = style.isDebossed,
            embossDepth = style.embossDepth,
            embossHighlightColor = style.embossHighlightColor ?: DEFAULT_HIGHLIGHT,
            embossShadowColor = style.embossShadowColor ?: DEFAULT_SHADOW,
            hasOuterGlow = style.hasOuterGlow,
            outerGlowColor = style.outerGlowColor ?: DEFAULT_OUTER_GLOW,
            outerGlowRadius = style.outerGlowRadius,
            outerGlowOpacity = style.outerGlowOpacity,
            hasInnerGlow = style.hasInnerGlow,
            innerGlowColor = style.innerGlowColor ?: Color.WHITE,
            innerGlowRadius = style.innerGlowRadius,
            innerGlowOpacity = style.innerGlowOpacity,
            hasShadow = hasShadowVal,
            shadowColor = style.shadowColor ?: Color.TRANSPARENT,
            shadowRadius = style.shadowRadius,
            shadowDx = style.shadowDx,
            shadowDy = style.shadowDy,
            shadowOpacity = style.shadowOpacity,
            hasLabel = style.hasLabel,
            labelShape = style.labelShape,
            labelColor = style.labelColor,
            labelGradient = style.labelGradient,
            labelSecondaryColor = style.labelSecondaryColor,
            labelStrokeColor = style.labelStrokeColor,
            labelStrokeWidth = style.labelStrokeWidth,
            hasGlossHighlight = style.hasGlossHighlight,
            hasFoldedRibbonFlaps = style.hasFoldedRibbonFlaps
        )
    }

    // The catalogue leaves an effect's colour out when it wants the stock one, so these
    // are the stock ones. Named here rather than parsed inline at both former call sites.
    private val DEFAULT_ANAGLYPH_1 = Color.parseColor("#FF0055")
    private val DEFAULT_ANAGLYPH_2 = Color.parseColor("#00E5FF")
    private val DEFAULT_HIGHLIGHT = Color.parseColor("#80FFFFFF")
    private val DEFAULT_SHADOW = Color.parseColor("#80000000")
    private val DEFAULT_OUTER_GLOW = Color.parseColor("#00E5FF")
}
