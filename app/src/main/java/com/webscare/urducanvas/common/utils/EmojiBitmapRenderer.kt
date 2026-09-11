package com.webscare.urducanvas.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.webscare.urducanvas.R

/**
 * Renders an emoji string to a square Bitmap using Android's full text
 * rendering pipeline (StaticLayout → Canvas).
 *
 * WHY StaticLayout:
 * canvas.drawText() bypasses Android's emoji rendering pipeline for complex
 * emoji sequences. StaticLayout uses the full text shaping engine including
 * NotoColorEmoji bitmap strikes — identical output to a TextView.
 *
 * WHY WE SHAPE SMALL AND SCALE UP:
 * Colour emoji are CBDT bitmap glyphs; NotoColorEmoji's strikes are 136×128px,
 * so laying text out at 512px asks the text pipeline for a glyph nine times
 * larger than anything the font holds, gains no detail, and is exactly the size
 * range where large-glyph rendering drops the mark and leaves a blank canvas —
 * which is what put invisible emoji on the artboard while the picker's TextView
 * (rendering at ~24sp) looked correct. We shape at [SHAPE_TEXT_SIZE_PX], then
 * scale the result up to the requested size with a filtered blit.
 *
 * WHY SQUARE OUTPUT:
 * Emoji are square glyphs. The canvas artboard should be 1:1.
 * We measure the real glyph dimensions, take the larger of width/height,
 * and produce a square bitmap of that size so the emoji is never distorted
 * or placed on a landscape/portrait artboard.
 */
object EmojiBitmapRenderer {

    private const val TAG = "EmojiBitmapRenderer"

    /** Comfortably above NotoColorEmoji's native strike size, well inside safe territory. */
    private const val SHAPE_TEXT_SIZE_PX = 160f

    /** Breathing room kept around the ink, as a fraction of its longer side, per edge. */
    private const val INK_MARGIN_RATIO = 0.08f

    /** Alpha at or above which a pixel counts as ink. Low, so antialiased edges survive. */
    private const val INK_ALPHA_FLOOR = 8

    /**
     * @param context    Used to load the same font the picker tile previews with.
     * @param emojiChar  Emoji string (may be multi-codepoint sequence)
     * @param sizePx     Side length of the square bitmap returned. Independent of the
     *                   size the glyph is actually shaped at.
     */
    fun render(context: Context, emojiChar: String, sizePx: Int = 512): Bitmap {
        val side = sizePx.coerceAtLeast(1)
        val typeface = typefaceFor(context, emojiChar)

        val shaped = shape(emojiChar, typeface)
        if (shaped != null && !isBlank(shaped)) {
            return scaleToSquare(trimToInk(shaped), side)
        }

        // Fallback: the same path the picker cell renders through. If StaticLayout
        // produced nothing, a plain drawText on a software canvas usually still does.
        Log.w(TAG, "StaticLayout produced a blank glyph for \"$emojiChar\" — falling back to drawText")
        val drawn = drawDirect(emojiChar, typeface)
        if (drawn != null) return scaleToSquare(trimToInk(drawn), side)

        Log.e(TAG, "Unable to render emoji \"$emojiChar\"; returning empty bitmap")
        return Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    }

    // ── Typeface ──────────────────────────────────────────────────────────────

    @Volatile private var symbolsTypeface: Typeface? = null
    @Volatile private var symbolsLoaded = false

    /** @font/symbols, loaded once off the application context so nothing is pinned. */
    private fun symbols(context: Context): Typeface? {
        if (symbolsLoaded) return symbolsTypeface
        synchronized(this) {
            if (!symbolsLoaded) {
                symbolsTypeface = try {
                    ResourcesCompat.getFont(context.applicationContext, R.font.symbols)
                } catch (e: Throwable) {
                    Log.w(TAG, "could not load @font/symbols", e)
                    null
                }
                symbolsLoaded = true
            }
        }
        return symbolsTypeface
    }

    /**
     * The font to draw [ch] with, or null to keep the platform default.
     *
     * The picker tiles declare `@font/symbols` (item_emoji.xml), and this renderer
     * used a bare TextPaint — so the two drew the same character from two different
     * fonts. Supplemental Arrows-C (U+1F800..U+1F8FF), the whole Arrows tab, is in
     * the bundled font and in no system font on most devices: the tile looked right
     * and the sticker landed on the canvas as tofu.
     *
     * Anything the default can already draw is left alone. Colour emoji are exactly
     * that case — they come from NotoColorEmoji through the default fallback chain,
     * which the symbols font has none of, and that path already works.
     */
    private fun typefaceFor(context: Context, ch: String): Typeface? {
        if (ch.isEmpty()) return null
        if (Paint().hasGlyph(ch)) return null
        val symbols = symbols(context) ?: return null
        return if (Paint().apply { typeface = symbols }.hasGlyph(ch)) symbols else null
    }

    // ── Shaping ───────────────────────────────────────────────────────────────

    /** Lays the string out with the full shaping engine and crops to the real glyph box. */
    private fun shape(emojiChar: String, face: Typeface?): Bitmap? {
        if (emojiChar.isEmpty()) return null

        val paint = TextPaint().apply {
            textSize = SHAPE_TEXT_SIZE_PX
            isAntiAlias = true
            color = Color.BLACK      // only relevant for monochrome glyphs
            face?.let { typeface = it }
        }

        @Suppress("DEPRECATION")
        val layout = StaticLayout(
            emojiChar,
            paint,
            (SHAPE_TEXT_SIZE_PX * 2).toInt(),
            android.text.Layout.Alignment.ALIGN_NORMAL,
            1f,
            0f,
            false
        )

        // layout.width is the full allocated width, not the glyph's. getLineWidth(0) is
        // how many pixels the single line actually used.
        val glyphW = layout.getLineWidth(0).toInt()
        val glyphH = layout.height
        if (glyphW <= 0 || glyphH <= 0) return null

        // Draw with room to spare on every side. Ink routinely falls outside the advance
        // width, and anything clipped here is gone before trimToInk can frame it.
        val pad = (SHAPE_TEXT_SIZE_PX / 2f).toInt()
        val box = maxOf(glyphW, glyphH) + pad * 2
        return try {
            val bmp = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.translate((box - glyphW) / 2f, (box - glyphH) / 2f)
            layout.draw(canvas)
            bmp
        } catch (e: Throwable) {
            Log.w(TAG, "StaticLayout draw failed for \"$emojiChar\"", e)
            null
        }
    }

    /** Last resort — measure with Paint and draw the glyph straight onto a software canvas. */
    private fun drawDirect(emojiChar: String, face: Typeface?): Bitmap? {
        if (emojiChar.isEmpty()) return null

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = SHAPE_TEXT_SIZE_PX
            color = Color.BLACK
            face?.let { typeface = it }
        }

        val bounds = Rect()
        paint.getTextBounds(emojiChar, 0, emojiChar.length, bounds)
        val glyphW = maxOf(bounds.width(), paint.measureText(emojiChar).toInt())
        val glyphH = bounds.height()
        if (glyphW <= 0 || glyphH <= 0) return null

        val box = maxOf(glyphW, glyphH)
        return try {
            val bmp = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            // getTextBounds is relative to the origin/baseline, so shift the glyph box
            // back to (0,0) before centring it.
            val x = (box - glyphW) / 2f - bounds.left
            val y = (box - glyphH) / 2f - bounds.top
            canvas.drawText(emojiChar, x, y, paint)
            bmp
        } catch (e: Throwable) {
            Log.w(TAG, "drawText fallback failed for \"$emojiChar\"", e)
            null
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Samples the bitmap on a coarse grid looking for any non-transparent pixel. A blank
     * result here is the silent failure this class exists to catch, so it is worth the
     * few hundred reads rather than shipping an invisible sticker to the canvas.
     */
    private fun isBlank(bmp: Bitmap): Boolean {
        val step = maxOf(1, minOf(bmp.width, bmp.height) / 24)
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < bmp.width) {
                if (Color.alpha(bmp.getPixel(x, y)) != 0) return false
                x += step
            }
            y += step
        }
        return true
    }

    /**
     * Crops a rendered glyph to its ink and re-centres it in a square with a small margin.
     *
     * Everything the layout reports is typographic rather than visual: the line height
     * carries ascent and descent sized for glyphs this string does not contain, and the
     * advance carries side bearings. Squaring off the larger of those two left the mark
     * adrift in a box much bigger than itself, which is the dead space that showed up
     * around an arrow dropped on the canvas — and the same around shapes and emoji, since
     * they all come through here.
     *
     * Alpha is the measure, so it works for colour emoji as well as monochrome marks.
     *
     * The margin is deliberate and stays: a sticker cropped hard against its ink has no
     * breathing room and butts into whatever it is placed beside. The result is still
     * square, so the artboard it creates is 1:1 as before.
     */
    private fun trimToInk(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        // One bulk read — a per-pixel getPixel over a few hundred rows is far slower.
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)

        var top = h; var bottom = -1; var left = w; var right = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((px[row + x] ushr 24) >= INK_ALPHA_FLOOR) {
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
        }
        if (bottom < top || right < left) return source   // nothing inked — leave it alone

        val inkW = right - left + 1
        val inkH = bottom - top + 1
        val margin = (maxOf(inkW, inkH) * INK_MARGIN_RATIO).toInt()
        val box = maxOf(inkW, inkH) + margin * 2

        return try {
            val out = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val dstLeft = (box - inkW) / 2
            val dstTop = (box - inkH) / 2
            canvas.drawBitmap(
                source,
                Rect(left, top, right + 1, bottom + 1),
                Rect(dstLeft, dstTop, dstLeft + inkW, dstTop + inkH),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            )
            source.recycle()
            out
        } catch (e: Throwable) {
            Log.w(TAG, "ink trim failed", e)
            source
        }
    }

    private fun scaleToSquare(source: Bitmap, side: Int): Bitmap {
        if (source.width == side && source.height == side) return source
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(source, null, RectF(0f, 0f, side.toFloat(), side.toFloat()), paint)
        source.recycle()
        return out
    }
}
