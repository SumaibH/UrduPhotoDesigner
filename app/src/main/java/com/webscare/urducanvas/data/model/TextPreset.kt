package com.webscare.urducanvas.data.model

import com.google.gson.JsonObject

/**
 * A finished text lockup — two or three phrases, each in its own font and style,
 * arranged into one composition the user drops on the canvas and then edits.
 *
 * The difference from [TextStylePreset] is what it covers. A style paints one text
 * element; a preset is an arrangement of several, and it carries no paint of its own —
 * every layer names a style by id. That indirection is the whole design: the styles are
 * the building blocks, so improving a style improves every preset wearing it, and a
 * preset can never drift from the style it names because both go through
 * [com.webscare.urducanvas.common.canvas.TextStyleApplier].
 *
 * Presets are text only. An earlier draft gave each one an optional background; that was
 * dropped, so dropping a preset onto a canvas can never disturb a photo already there.
 * Preset *cards* still draw on a panel, but that is the thumbnail's presentation and
 * nothing to do with this data.
 */
data class TextPreset(
    val id: String,
    val name: String,
    val category: TextPresetCategory,

    /**
     * Width divided by height of the box the layout was composed in.
     *
     * Layer positions are fractions of this box, never pixels, so one preset drops
     * correctly onto a square post and onto a story — the box is sized to the canvas at
     * insertion and the fractions still mean the same thing.
     */
    val aspect: Float = 1f,

    val layers: List<PresetLayer> = emptyList()
) {
    /** Every font this lockup needs on disk before it can be inserted at full fidelity. */
    val fontIds: List<String> get() = layers.mapNotNull { it.fontId }.distinct()

    /**
     * How tall layer [index] may be drawn, as a fraction of the box height.
     *
     * A layer's size is solved from its width — measure the line, scale it to
     * [PresetLayer.widthPct] of the box — and width alone says nothing about height. A
     * short word given a wide target becomes enormous and lands on top of the line
     * below it, which is what "ماہِ صیام / مبارک" did: two words, one of them brief, and
     * the lockup rendered as a pile.
     *
     * Two limits, whichever is tighter. A line centred halfway between its neighbour
     * may be at most as tall as the distance between their centres — each spends half
     * its height reaching toward the other, so equal heights exactly meet. And it may
     * not reach past the edge of the box, which is twice its distance to the nearer one.
     */
    fun verticalRoom(index: Int): Float {
        val layer = layers.getOrNull(index) ?: return 1f
        val toNeighbour = layers.asSequence()
            .filterIndexed { i, _ -> i != index }
            .map { kotlin.math.abs(it.yPct - layer.yPct) }
            .minOrNull() ?: 1f
        val toEdge = 2f * minOf(layer.yPct, 1f - layer.yPct)
        return minOf(toNeighbour, toEdge).coerceIn(0.05f, 1f)
    }

    /**
     * Whether this preset needs a subscription: it does if any font or style it uses does.
     *
     * Computed from the current premium sets rather than stored in the JSON, so a preset
     * re-prices itself the moment a font's flag changes on the dashboard — no content
     * re-release. Dormant at the time of writing: all 261 fonts are marked free, so this
     * returns false for everything until flags are flipped server-side.
     *
     * Pure, and takes the sets rather than reaching for them, so it cannot go stale
     * behind a cached list the way a mutable flag would.
     */
    fun isPremium(premiumFontIds: Set<String>, premiumStyleIds: Set<String>): Boolean =
        layers.any { it.fontId != null && it.fontId in premiumFontIds } ||
                layers.any { it.styleId != null && it.styleId in premiumStyleIds }
}

/**
 * One phrase in a lockup: what it says, what it wears, and where it sits.
 */
data class PresetLayer(
    /** The Urdu (or English) the layer renders. Editable once inserted. */
    val text: String,

    /** [FontEntity.file_name] — the font's identity everywhere else in the app. */
    val fontId: String? = null,

    /** [TextStylePreset.id]. An id that no longer resolves leaves the layer unstyled. */
    val styleId: String? = null,

    /**
     * Style fields to change for this layer only, as raw JSON.
     *
     * Deliberately untyped. It is merged onto the style's JSON before that is
     * deserialised, so it can name any field the style format has — including fields
     * added to the format later, which a typed override class would have to be taught
     * about first. Lets one style dress two layers that differ only in colour.
     */
    val override: JsonObject? = null,

    /** Centre of the layer, as a fraction of the preset's box. */
    val xPct: Float = 0.5f,
    val yPct: Float = 0.5f,

    /** How wide the line should measure, as a fraction of the box. Solves the font size. */
    val widthPct: Float = 0.8f,

    val rotation: Float = 0f,
    val align: String = "CENTER"
)
