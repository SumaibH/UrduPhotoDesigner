package com.webscare.urducanvas.ui.editor.panels.text.styles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.enums.LabelShape
import com.webscare.urducanvas.data.model.PresetCategory
import com.webscare.urducanvas.data.model.TextPreset
import com.webscare.urducanvas.data.model.TextStylePreset
import com.webscare.urducanvas.data.repository.TextPresetsRepository
import kotlin.math.min

object TextStyleThumbnailRenderer {

    /** The box every thumbnail is drawn in, and the unit the drawing code is written in. */
    private const val THUMB_PX = 180

    /** What a style tile spells out. One word, so the effects are what the eye reads. */
    private const val SAMPLE_TEXT = "اردو"
    private const val SAMPLE_TEXT_SIZE = 58f

    /** The two plates a lockup card can sit on, and how far their corners are rounded. */
    private val PANEL_LIGHT = Color.parseColor("#F2F3F0")
    private val PANEL_DARK = Color.parseColor("#1E211F")
    private const val PANEL_RADIUS = 12f

    /** Above this mean luminance the ink needs the dark plate to be visible at all. */
    private const val LIGHT_INK_THRESHOLD = 0.62f

    /** Below this a line is a smudge rather than a word, so it stops shrinking. */
    private const val MIN_LAYER_TEXT_PX = 9f

    /** A little under the room a layer is owed, so neighbours have air rather than touch. */
    private const val LAYER_ROOM_SLACK = 0.88f

    private val thumbnailCache = LruCache<String, Bitmap>(200)

    fun clearCache() {
        thumbnailCache.evictAll()
    }

    fun getCachedOrGenerateThumbnail(
        context: Context,
        preset: TextStylePreset,
        customTypeface: Typeface? = null,
        fontKey: String? = null
    ): Bitmap {
        val key = "${preset.id}_${fontKey ?: "default"}"
        thumbnailCache.get(key)?.let { return it }

        val bmp = generatePresetThumbnail(context, preset, customTypeface)
        thumbnailCache.put(key, bmp)
        return bmp
    }

    /**
     * A one-off render for the in-panel asset preview, which shows a preset several
     * times the size of a tile. Not cached: it is one bitmap at a time, and putting
     * something this big in the tile cache would evict most of the grid.
     */
    fun renderForPreview(
        context: Context,
        preset: TextStylePreset,
        sizePx: Int,
        customTypeface: Typeface? = null
    ): Bitmap = generatePresetThumbnail(context, preset, customTypeface, sizePx)

    private fun generatePresetThumbnail(
        context: Context,
        preset: TextStylePreset,
        customTypeface: Typeface? = null,
        sizePx: Int = THUMB_PX
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Everything below is written against the 180pt box the tiles use. Drawing
        // through a scale keeps one set of numbers and still comes out sharp when
        // the preview asks for a bitmap several times that size — text and paths
        // are re-rasterised at the larger size rather than stretched.
        if (sizePx != THUMB_PX) {
            val s = sizePx / THUMB_PX.toFloat()
            canvas.scale(s, s)
        }

        drawStyledText(
            canvas = canvas,
            preset = preset,
            text = SAMPLE_TEXT,
            typeface = customTypeface ?: defaultTypeface(context),
            textSizePx = SAMPLE_TEXT_SIZE,
            centerX = THUMB_PX / 2f,
            centerY = THUMB_PX / 2f,
            boxW = THUMB_PX.toFloat(),
            boxH = THUMB_PX.toFloat()
        )
        return bitmap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lockups — several styled lines arranged into one card
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renders [preset]'s whole lockup onto a card.
     *
     * Cached by id and size. The key also carries how many of the preset's fonts were
     * on disk at render time, so when a download finishes the card re-renders against
     * the real face instead of keeping the fallback it was drawn with — without having
     * to hunt down and evict individual entries.
     */
    fun getCachedOrGenerateLockup(
        context: Context,
        preset: TextPreset,
        typefaces: Map<String, Typeface>,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val key = "lockup_${preset.id}_${widthPx}x${heightPx}_${typefaces.size}"
        thumbnailCache.get(key)?.let { return it }

        val bmp = generateLockupThumbnail(context, preset, typefaces, widthPx, heightPx)
        thumbnailCache.put(key, bmp)
        return bmp
    }

    private fun generateLockupThumbnail(
        context: Context,
        preset: TextPreset,
        typefaces: Map<String, Typeface>,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val fallback = defaultTypeface(context)
        val boxW = bitmap.width.toFloat()
        val boxH = bitmap.height.toFloat()

        // Resolved once, because the panel has to be chosen from them before anything
        // is drawn on it.
        val styles = preset.layers.map { layer ->
            TextPresetsRepository.resolveLayerStyle(context, layer)
                ?: TextStylePreset.none(PresetCategory.MINIMAL)
        }

        // The panel. Presets are text only — nothing here reaches the canvas — but a
        // lockup drawn on nothing is unreadable half the time, and so is one drawn on a
        // plate of the wrong tone: the catalogue runs from white letterpress to black
        // minimal, and a single neutral card makes one end of that range disappear. So
        // the plate is picked against the lockup's own ink rather than fixed, and the
        // card reads as a finished post either way.
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (wantsDarkPanel(styles)) PANEL_DARK else PANEL_LIGHT
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            PANEL_RADIUS, PANEL_RADIUS, panelPaint
        )

        preset.layers.forEachIndexed { index, layer ->
            // A style that no longer resolves leaves the line unstyled rather than
            // dropping it — the words are most of what the card is for.
            val style = styles[index]
            val typeface = layer.fontId?.let { typefaces[it] } ?: fallback

            // The authored width is a fraction of the box, so the size is solved rather
            // than authored: measure the line at a reference size and scale by how far
            // off the target it lands. One measure, no search loop.
            val target = boxW * layer.widthPct
            val probe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = SAMPLE_TEXT_SIZE
                this.typeface = typeface
            }
            val measured = probe.measureText(layer.text)
            var solvedSize = if (measured > 0f) {
                (SAMPLE_TEXT_SIZE * (target / measured)).coerceIn(MIN_LAYER_TEXT_PX, boxH * 0.6f)
            } else SAMPLE_TEXT_SIZE

            // Then held to the room the layout leaves it — but measured as ink, not as
            // font metrics. A face's ascent-to-descent covers every glyph it can draw,
            // and for Nastaliq that is a long way below the baseline that most words
            // never reach; capping against it shrank every lockup to a timid little
            // line in the middle of an empty card. What can actually collide is the ink.
            probe.textSize = solvedSize
            val ink = Rect()
            probe.getTextBounds(layer.text, 0, layer.text.length, ink)
            val inkHeight = ink.height().toFloat()
            val roomPx = preset.verticalRoom(index) * boxH * LAYER_ROOM_SLACK
            if (inkHeight > roomPx && inkHeight > 0f) {
                solvedSize = (solvedSize * (roomPx / inkHeight)).coerceAtLeast(MIN_LAYER_TEXT_PX)
            }

            canvas.withRotationAbout(layer.rotation, boxW * layer.xPct, boxH * layer.yPct) {
                drawStyledText(
                    canvas = canvas,
                    preset = style,
                    text = layer.text,
                    typeface = typeface,
                    textSizePx = solvedSize,
                    centerX = boxW * layer.xPct,
                    centerY = boxH * layer.yPct,
                    boxW = boxW,
                    boxH = boxH
                )
            }
        }
        return bitmap
    }

    /**
     * Whether every line in a lockup wants the dark plate.
     *
     * Only if every one does. A card carries one plate, and a dark line on a dark plate
     * disappears completely, while a light line on a light plate usually survives on its
     * shadow and stroke — so a lockup that mixes the two gets the light plate, which is
     * also the rest of the panel's colour.
     *
     * What "wants dark" means, per style:
     *  - a glow does. An outer glow is light bleeding past the letter, and on a light
     *    plate there is nothing for it to bleed into.
     *  - light ink with no dark contour does. Gold, white neon, pale metal — nothing in
     *    them draws the letterform except the fill itself.
     *  - light ink *with* a dark contour does not. A white letterpress is read by its
     *    dark stroke, not its fill; put it on black and the stroke is what vanishes.
     */
    private fun wantsDarkPanel(styles: List<TextStylePreset>): Boolean {
        if (styles.isEmpty()) return false
        return styles.all { style ->
            if (style.hasOuterGlow && style.outerGlowRadius > 0f) return@all true

            // Gradients are read by the middle of the ramp: a metal runs dark at both
            // ends and it is the highlight band that the eye takes as its colour.
            val fill = style.textGradient?.colors?.let { it[it.size / 2] }
                ?: style.textColor?.takeIf { it != Color.TRANSPARENT }
                ?: return@all false
            if (luminance(fill) <= LIGHT_INK_THRESHOLD) return@all false

            val contour = style.strokeColor?.takeIf { style.strokeWidth > 0f }
            contour == null || luminance(contour) > LIGHT_INK_THRESHOLD
        }
    }

    /** Perceived brightness, 0..1. Green reads far lighter than blue at the same value. */
    private fun luminance(color: Int): Float =
        (0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)) / 255f

    /** Runs [block] with the canvas rotated about a point, then restores it. */
    private inline fun Canvas.withRotationAbout(degrees: Float, px: Float, py: Float, block: () -> Unit) {
        if (degrees == 0f) {
            block()
            return
        }
        val saved = save()
        rotate(degrees, px, py)
        block()
        restoreToCount(saved)
    }

    private fun defaultTypeface(context: Context): Typeface = try {
        ResourcesCompat.getFont(context, R.font.default_canvas) ?: Typeface.DEFAULT_BOLD
    } catch (e: Exception) {
        Typeface.DEFAULT_BOLD
    }

    /**
     * Draws one line of text wearing one style, centred on ([centerX], [centerY]).
     *
     * Every effect the catalogue can express is drawn here, in the order they stack —
     * label, extrusions, shadow and glow, strokes, the fill, then the inner glow. It was
     * the body of the tile renderer and drew a fixed sample word at the middle of a fixed
     * box; a lockup is several of these at different places and sizes, so the text, the
     * face, the size and the position all became arguments. The tile is now just the case
     * where there is one of them, in the middle.
     *
     * [boxW] and [boxH] bound the label plate, which is the only effect that has an
     * extent of its own rather than following the glyphs.
     */
    private fun drawStyledText(
        canvas: Canvas,
        preset: TextStylePreset,
        text: String,
        typeface: Typeface,
        textSizePx: Float,
        centerX: Float,
        centerY: Float,
        boxW: Float,
        boxH: Float
    ) {
        val width = boxW
        val height = boxH

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }

        val textWidth = textPaint.measureText(text)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val cx = centerX
        // Callers hand over the centre of the line; drawText wants its baseline.
        val cy = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f

        // ── LAYER 0: LABEL BACKGROUND ─────────────────────────────────────────
        if (preset.hasLabel) {
            val padX = 22f
            val padY = 12f
            val labelRect = RectF(
                (cx - textWidth / 2f - padX).coerceAtLeast(8f),
                (cy - textHeight / 2f - padY).coerceAtLeast(8f),
                (cx + textWidth / 2f + padX).coerceAtMost(width - 8f),
                (cy + textHeight / 2f + padY).coerceAtMost(height - 8f)
            )

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = if (isStrokeShape(preset.labelShape)) Paint.Style.STROKE else Paint.Style.FILL
                if (isStrokeShape(preset.labelShape)) {
                    strokeWidth = 3f
                }
            }

            if (preset.labelGradient != null) {
                val colors = preset.labelGradient.colors.toIntArray()
                labelPaint.shader = LinearGradient(
                    labelRect.left, labelRect.top, labelRect.right, labelRect.bottom,
                    colors, null, Shader.TileMode.CLAMP
                )
            } else {
                labelPaint.color = preset.labelColor
            }

            // Draw Ribbon Fold Flaps if present
            if (preset.hasFoldedRibbonFlaps && preset.labelSecondaryColor != null) {
                val flapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = preset.labelSecondaryColor
                    style = Paint.Style.FILL
                }
                val flapPath = Path().apply {
                    moveTo(labelRect.left, labelRect.bottom)
                    lineTo(labelRect.left - 10f, labelRect.bottom + 6f)
                    lineTo(labelRect.left + 8f, labelRect.bottom)
                    close()
                    moveTo(labelRect.right, labelRect.top)
                    lineTo(labelRect.right + 10f, labelRect.top - 6f)
                    lineTo(labelRect.right - 8f, labelRect.top)
                    close()
                }
                canvas.drawPath(flapPath, flapPaint)
            }

            // Render Exact Shape
            drawLabelShape(canvas, preset.labelShape, labelRect, labelPaint)

            // Inner Stroke / Border
            if (preset.labelStrokeColor != null && preset.labelStrokeWidth > 0f) {
                val innerStrokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = preset.labelStrokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = preset.labelStrokeWidth
                }
                val inset = preset.labelStrokeWidth * 1.5f + 2f
                val insetRect = RectF(
                    labelRect.left + inset,
                    labelRect.top + inset,
                    labelRect.right - inset,
                    labelRect.bottom - inset
                )
                canvas.drawRoundRect(insetRect, 10f, 10f, innerStrokeP)
            }

            // Glossy Shine Highlight
            if (preset.hasGlossHighlight) {
                val glossP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        labelRect.left, labelRect.top, labelRect.left, labelRect.centerY(),
                        Color.argb(120, 255, 255, 255), Color.argb(10, 255, 255, 255),
                        Shader.TileMode.CLAMP
                    )
                }
                val glossRect = RectF(labelRect.left + 1f, labelRect.top + 1f, labelRect.right - 1f, labelRect.centerY())
                canvas.drawRoundRect(glossRect, 10f, 10f, glossP)
            }
        }

        // ── LAYER 1a: DOUBLE STEP 2 EXTRUSION ────────────────────────────────
        if (preset.hasDoubleExtrude && preset.extrudeStep2Depth > 0f) {
            val step2Paint = Paint(textPaint).apply {
                shader = null
                color = preset.extrudeStep2Color ?: Color.BLACK
                maskFilter = null
                style = Paint.Style.FILL
            }
            val steps = (preset.extrudeStep2Depth.toInt()).coerceIn(1, 16)
            for (step in 1..steps) {
                val stepFrac = step.toFloat() / steps
                val ex = cx + preset.extrudeStep2Dx * stepFrac
                val ey = cy + preset.extrudeStep2Dy * stepFrac
                canvas.drawText(text, ex, ey, step2Paint)
            }
        }

        // ── LAYER 1b: 3D BLOCK EXTRUSION / HARD OFFSET LAYER ──────────────────
        if (preset.has3dExtrude) {
            val extrudePaint = Paint(textPaint).apply {
                shader = null
                color = preset.extrudeColor ?: Color.BLACK
                maskFilter = null
                style = Paint.Style.FILL
            }
            val depth = if (preset.extrudeDepth > 0f) preset.extrudeDepth else kotlin.math.hypot(preset.extrudeDx, preset.extrudeDy)
            val steps = (depth.toInt()).coerceIn(1, 16)
            for (step in 1..steps) {
                val stepFrac = step.toFloat() / steps
                val ex = cx + preset.extrudeDx * stepFrac
                val ey = cy + preset.extrudeDy * stepFrac
                canvas.drawText(text, ex, ey, extrudePaint)
            }
        }

        // ── LAYER 2: SHADOW / SOFT GLOW / HARD OFFSET DROP ───────────────────
        if (preset.shadowColor != null && (preset.shadowRadius > 0f || preset.shadowDx != 0f || preset.shadowDy != 0f)) {
            val baseAlpha = Color.alpha(preset.shadowColor).takeIf { it > 0 } ?: 255
            val effectiveAlpha = ((preset.shadowOpacity.coerceIn(0, 255) / 255f) * baseAlpha).toInt()
            val sc = (preset.shadowColor and 0x00FFFFFF) or (effectiveAlpha shl 24)
            val shadowPaint = Paint(textPaint).apply {
                shader = null
                color = sc
                maskFilter = if (preset.shadowRadius > 0.5f) BlurMaskFilter(preset.shadowRadius, BlurMaskFilter.Blur.NORMAL) else null
            }
            canvas.drawText(text, cx + preset.shadowDx, cy + preset.shadowDy, shadowPaint)
        }

        // ── LAYER 2b: OUTER GLOW ──────────────────────────────────────────────
        if (preset.hasOuterGlow && preset.outerGlowRadius > 0f && preset.outerGlowOpacity > 0) {
            val baseAlpha = Color.alpha(preset.outerGlowColor ?: Color.CYAN).takeIf { it > 0 } ?: 255
            val effectiveAlpha = ((preset.outerGlowOpacity.coerceIn(0, 255) / 255f) * baseAlpha).toInt()
            val glowCol = ((preset.outerGlowColor ?: Color.CYAN) and 0x00FFFFFF) or (effectiveAlpha shl 24)
            val glowPaint = Paint(textPaint).apply {
                shader = null
                color = glowCol
                maskFilter = BlurMaskFilter(preset.outerGlowRadius.coerceAtLeast(0.5f), BlurMaskFilter.Blur.OUTER)
            }
            canvas.drawText(text, cx, cy, glowPaint)
        }

        // ── LAYER 3: OUTER UNDER-STROKE / SECONDARY CONTOUR ──────────────────
        val effectiveUnderStrokeWidth = if (preset.hasUnderStroke && preset.underStrokeWidth > 0f) {
            preset.underStrokeWidth
        } else if (preset.strokeWidth > 0f && preset.strokeColor != null && preset.textColor != Color.TRANSPARENT && preset.textGradient == null) {
            preset.strokeWidth * 1.8f
        } else 0f

        val effectiveUnderStrokeColor = preset.underStrokeColor ?: preset.strokeColor

        if (effectiveUnderStrokeWidth > 0f && effectiveUnderStrokeColor != null) {
            val underStrokePaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = effectiveUnderStrokeWidth
                color = effectiveUnderStrokeColor
                shader = null
                maskFilter = null
            }
            canvas.drawText(text, cx, cy, underStrokePaint)
        }

        // ── LAYER 4: INNER STROKE / PRIMARY STROKE ────────────────────────────
        val isStrokeOnly = preset.textColor == Color.TRANSPARENT && preset.textGradient == null
        if (isStrokeOnly && preset.strokeColor != null) {
            val strokePaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = preset.strokeWidth.takeIf { it > 0f } ?: 2.5f
                color = preset.strokeColor
                shader = null
                maskFilter = null
            }
            canvas.drawText(text, cx, cy, strokePaint)
        } else if (preset.strokeColor != null && preset.strokeWidth > 0f && !preset.hasUnderStroke) {
            val strokePaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = preset.strokeWidth
                color = preset.strokeColor
                shader = null
                maskFilter = null
            }
            canvas.drawText(text, cx, cy, strokePaint)
        }

        // ── LAYER 5a: ANAGLYPH 3D STEREOSCOPIC SPLIT ──────────────────────────
        if (preset.hasAnaglyph && preset.anaglyphOffset > 0f) {
            val anaglyphPaint1 = Paint(textPaint).apply {
                shader = null
                color = preset.anaglyphColor1 ?: Color.parseColor("#FF0055")
                maskFilter = null
            }
            val anaglyphPaint2 = Paint(textPaint).apply {
                shader = null
                color = preset.anaglyphColor2 ?: Color.parseColor("#00E5FF")
                maskFilter = null
            }
            canvas.drawText(text, cx - preset.anaglyphOffset, cy, anaglyphPaint1)
            canvas.drawText(text, cx + preset.anaglyphOffset, cy, anaglyphPaint2)
        }

        // ── LAYER 5b: 3D CHISEL BEVEL ─────────────────────────────────────────
        if (preset.hasBevel && preset.bevelDepth > 0f) {
            val bevelShadowPaint = Paint(textPaint).apply {
                shader = null
                color = preset.bevelShadowColor ?: Color.parseColor("#80000000")
                maskFilter = null
            }
            val bevelHighlightPaint = Paint(textPaint).apply {
                shader = null
                color = preset.bevelHighlightColor ?: Color.parseColor("#80FFFFFF")
                maskFilter = null
            }
            canvas.drawText(text, cx + preset.bevelDepth, cy + preset.bevelDepth, bevelShadowPaint)
            canvas.drawText(text, cx - preset.bevelDepth, cy - preset.bevelDepth, bevelHighlightPaint)
        }

        // ── LAYER 5c: 3D EMBOSS & DEBOSS ──────────────────────────────────────
        if (preset.hasEmboss && preset.embossDepth > 0f) {
            val highlightPaint = Paint(textPaint).apply {
                shader = null
                color = preset.embossHighlightColor ?: Color.parseColor("#80FFFFFF")
                maskFilter = null
            }
            val shadowPaint = Paint(textPaint).apply {
                shader = null
                color = preset.embossShadowColor ?: Color.parseColor("#80000000")
                maskFilter = null
            }
            if (preset.isDebossed) {
                canvas.drawText(text, cx - preset.embossDepth, cy - preset.embossDepth, shadowPaint)
                canvas.drawText(text, cx + preset.embossDepth, cy + preset.embossDepth, highlightPaint)
            } else {
                canvas.drawText(text, cx - preset.embossDepth, cy - preset.embossDepth, highlightPaint)
                canvas.drawText(text, cx + preset.embossDepth, cy + preset.embossDepth, shadowPaint)
            }
        }

        // ── LAYER 5d: MAIN FILL ───────────────────────────────────────────────
        if (!isStrokeOnly) {
            if (preset.textGradient != null) {
                val colors = preset.textGradient.colors.toIntArray()
                textPaint.shader = LinearGradient(
                    cx - 35f, cy - 20f, cx + 35f, cy + 20f,
                    colors, null, Shader.TileMode.CLAMP
                )
            } else {
                textPaint.shader = null
                textPaint.color = preset.textColor ?: Color.BLACK
            }
            textPaint.maskFilter = null
            canvas.drawText(text, cx, cy, textPaint)
        }

        // ── LAYER 5e: INNER GLOW ──────────────────────────────────────────────
        if (preset.hasInnerGlow && preset.innerGlowRadius > 0f && preset.innerGlowOpacity > 0) {
            val baseAlpha = Color.alpha(preset.innerGlowColor ?: Color.WHITE).takeIf { it > 0 } ?: 255
            val effectiveAlpha = ((preset.innerGlowOpacity.coerceIn(0, 255) / 255f) * baseAlpha).toInt()
            val glowCol = ((preset.innerGlowColor ?: Color.WHITE) and 0x00FFFFFF) or (effectiveAlpha shl 24)
            val innerGlowPaint = Paint(textPaint).apply {
                shader = null
                color = glowCol
                maskFilter = BlurMaskFilter(preset.innerGlowRadius.coerceAtLeast(0.5f), BlurMaskFilter.Blur.INNER)
            }
            canvas.drawText(text, cx, cy, innerGlowPaint)
        }

    }

    private fun drawLabelShape(canvas: Canvas, shape: LabelShape, rect: RectF, paint: Paint) {
        when (shape) {
            LabelShape.RECTANGLE_FILL, LabelShape.RECTANGLE_STROKE -> {
                canvas.drawRect(rect, paint)
            }

            LabelShape.OVAL_FILL, LabelShape.OVAL_STROKE -> {
                canvas.drawOval(rect, paint)
            }

            LabelShape.CIRCLE_FILL, LabelShape.CIRCLE_STROKE -> {
                val r = min(rect.width(), rect.height()) / 2f
                canvas.drawCircle(rect.centerX(), rect.centerY(), r, paint)
            }

            LabelShape.ROUNDED_RECTANGLE_FILL, LabelShape.ROUNDED_RECTANGLE_STROKE -> {
                canvas.drawRoundRect(rect, 14f, 14f, paint)
            }

            LabelShape.CAPSULE_FILL, LabelShape.CAPSULE_STROKE -> {
                val pillRadius = min(rect.width(), rect.height()) / 2f
                canvas.drawRoundRect(rect, pillRadius, pillRadius, paint)
            }

            LabelShape.TAG_FILL, LabelShape.TAG_STROKE -> {
                val arrowWidth = rect.height() * 0.35f
                val path = Path().apply {
                    moveTo(rect.left, rect.top)
                    lineTo(rect.right - arrowWidth, rect.top)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.right - arrowWidth, rect.bottom)
                    lineTo(rect.left, rect.bottom)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.REVERSE_TAG_FILL, LabelShape.REVERSE_TAG_STROKE -> {
                val arrowWidth = rect.height() * 0.35f
                val path = Path().apply {
                    moveTo(rect.left + arrowWidth, rect.top)
                    lineTo(rect.right, rect.top)
                    lineTo(rect.right, rect.bottom)
                    lineTo(rect.left + arrowWidth, rect.bottom)
                    lineTo(rect.left, rect.centerY())
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.RIBBON_FILL, LabelShape.RIBBON_STROKE -> {
                val notch = rect.height() * 0.25f
                val path = Path().apply {
                    moveTo(rect.left, rect.top)
                    lineTo(rect.left + notch, rect.centerY())
                    lineTo(rect.left, rect.bottom)
                    lineTo(rect.right - notch, rect.bottom)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.right - notch, rect.top)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.SLANTED_FILL, LabelShape.SLANTED_STROKE -> {
                val slant = rect.height() * 0.3f
                val path = Path().apply {
                    moveTo(rect.left + slant, rect.top)
                    lineTo(rect.right, rect.top)
                    lineTo(rect.right - slant, rect.bottom)
                    lineTo(rect.left, rect.bottom)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.BADGE_FILL, LabelShape.BADGE_STROKE -> {
                val chamfer = min(rect.width(), rect.height()) * 0.22f
                val path = Path().apply {
                    moveTo(rect.left + chamfer, rect.top)
                    lineTo(rect.right - chamfer, rect.top)
                    lineTo(rect.right, rect.top + chamfer)
                    lineTo(rect.right, rect.bottom - chamfer)
                    lineTo(rect.right - chamfer, rect.bottom)
                    lineTo(rect.left + chamfer, rect.bottom)
                    lineTo(rect.left, rect.bottom - chamfer)
                    lineTo(rect.left, rect.top + chamfer)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.HEXAGON_BADGE_FILL, LabelShape.HEXAGON_BADGE_STROKE -> {
                val hex = rect.height() * 0.28f
                val path = Path().apply {
                    moveTo(rect.left + hex, rect.top)
                    lineTo(rect.right - hex, rect.top)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.right - hex, rect.bottom)
                    lineTo(rect.left + hex, rect.bottom)
                    lineTo(rect.left, rect.centerY())
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.DIAMOND_SHIELD_FILL, LabelShape.DIAMOND_SHIELD_STROKE -> {
                val path = Path().apply {
                    moveTo(rect.centerX(), rect.top)
                    lineTo(rect.right, rect.top + rect.height() * 0.25f)
                    lineTo(rect.centerX(), rect.bottom)
                    lineTo(rect.left, rect.top + rect.height() * 0.25f)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            LabelShape.UNDERLINE_BAR_FILL, LabelShape.UNDERLINE_BAR_STROKE -> {
                val barHeight = 6f
                val barRect = RectF(rect.left, rect.bottom - barHeight, rect.right, rect.bottom)
                canvas.drawRoundRect(barRect, 3f, 3f, paint)
            }

            LabelShape.SPEECH_BUBBLE_FILL, LabelShape.SPEECH_BUBBLE_STROKE -> {
                val path = Path().apply {
                    val rx = 14f
                    addRoundRect(RectF(rect.left, rect.top, rect.right, rect.bottom - 8f), rx, rx, Path.Direction.CW)
                    moveTo(rect.left + 22f, rect.bottom - 8f)
                    lineTo(rect.left + 14f, rect.bottom)
                    lineTo(rect.left + 34f, rect.bottom - 8f)
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun isStrokeShape(shape: LabelShape): Boolean {
        return shape in listOf(
            LabelShape.RECTANGLE_STROKE,
            LabelShape.OVAL_STROKE,
            LabelShape.CIRCLE_STROKE,
            LabelShape.ROUNDED_RECTANGLE_STROKE,
            LabelShape.CAPSULE_STROKE,
            LabelShape.TAG_STROKE,
            LabelShape.REVERSE_TAG_STROKE,
            LabelShape.RIBBON_STROKE,
            LabelShape.SLANTED_STROKE,
            LabelShape.BADGE_STROKE,
            LabelShape.HEXAGON_BADGE_STROKE,
            LabelShape.DIAMOND_SHIELD_STROKE,
            LabelShape.UNDERLINE_BAR_STROKE,
            LabelShape.SPEECH_BUBBLE_STROKE
        )
    }
}
