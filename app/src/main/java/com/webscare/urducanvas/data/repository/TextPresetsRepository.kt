package com.webscare.urducanvas.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.webscare.urducanvas.data.model.PresetLayer
import com.webscare.urducanvas.data.model.TextPreset
import com.webscare.urducanvas.data.model.TextPresetCategory
import com.webscare.urducanvas.data.model.TextStylePreset
import java.io.InputStreamReader

/**
 * The lockup catalogue, mirroring [TextStylesRepository].
 *
 * One file per category under `assets/presets/lockups/`, loaded when that category is
 * first opened rather than all at once — ten categories of fifty lockups is a lot of JSON
 * to parse on a panel that the user may never scroll past the first tab of, and the
 * editor's start-up budget is already spent.
 */
object TextPresetsRepository {

    private const val DIR = "presets/lockups"
    private const val TAG = "TextPresetsRepository"

    private val gson = Gson()

    /** Parsed categories, by category. A category present with an empty list was tried and had nothing. */
    private val cached: MutableMap<TextPresetCategory, List<TextPreset>> = mutableMapOf()

    /**
     * The lockups in [category], newest content first as authored.
     *
     * A category with no file yet returns empty — which is the state every category is in
     * until the content run lands, and is not an error.
     */
    @Synchronized
    fun getPresetsByCategory(context: Context, category: TextPresetCategory): List<TextPreset> {
        cached[category]?.let { return it }
        val parsed = load(context, category)
        cached[category] = parsed
        return parsed
    }

    /** Every lockup in every category. Loads all ten files, so not for a hot path. */
    fun getAllPresets(context: Context): List<TextPreset> =
        TextPresetCategory.values().flatMap { getPresetsByCategory(context, it) }

    /**
     * The lockup [id] names, or null.
     *
     * Null rather than a substitute, for the same reason
     * [TextStylesRepository.findPresetById] returns null: a recents shelf can name a
     * lockup a content update has since removed, and the honest answer is that it is gone.
     */
    fun findPresetById(context: Context, id: String): TextPreset? {
        if (id.isBlank()) return null
        // The id is prefixed with its category by convention (ramadan_006), so the right
        // file is usually the first one tried and the other nine are never parsed.
        val guess = TextPresetCategory.values()
            .firstOrNull { id.startsWith(it.name.lowercase() + "_") }
        guess?.let { cat ->
            getPresetsByCategory(context, cat).firstOrNull { it.id == id }?.let { return it }
        }
        return getAllPresets(context).firstOrNull { it.id == id }
    }

    /** Resolves [ids] in order, dropping any the catalogue no longer has. */
    fun findPresetsByIds(context: Context, ids: List<String>): List<TextPreset> =
        ids.mapNotNull { findPresetById(context, it) }

    /**
     * The style a layer wears, with its override merged in — or null when the layer names
     * no style, or names one that no longer exists.
     *
     * A null here means "leave this layer unstyled" and never "fail the insertion". The
     * layer still carries text and a font, which is most of what the user asked for.
     */
    fun resolveLayerStyle(context: Context, layer: PresetLayer): TextStylePreset? =
        TextStylesRepository.resolveStyle(context, layer.styleId, layer.override)

    /** Drops every parsed category. For tests and for a content refresh. */
    @Synchronized
    fun clearCache() = cached.clear()

    private fun load(context: Context, category: TextPresetCategory): List<TextPreset> {
        val fileName = "$DIR/${category.name.lowercase()}.json"
        return try {
            context.assets.open(fileName).use { stream ->
                val array = gson.fromJson(InputStreamReader(stream), JsonArray::class.java)
                    ?: return emptyList()
                array.mapNotNull { element -> parse(element as? JsonObject, category) }
            }
        } catch (e: Exception) {
            // A missing file is the normal state of a category with no content yet, and a
            // malformed one must cost that category rather than the panel. Either way the
            // shelf shows empty and the app carries on.
            Log.i(TAG, "No lockups for ${category.name}: ${e.message}")
            emptyList()
        }
    }

    private fun parse(obj: JsonObject?, category: TextPresetCategory): TextPreset? {
        if (obj == null) return null
        return try {
            val id = obj.get("id")?.asString ?: return null
            val layers = obj.getAsJsonArray("layers")?.mapNotNull { parseLayer(it as? JsonObject) }
                ?: emptyList()
            // A lockup with no layers would insert nothing and read as a broken card.
            if (layers.isEmpty()) return null

            TextPreset(
                id = id,
                name = obj.get("name")?.asString ?: id,
                // The category comes from which file it is in, not from the entry. The
                // field may still be present for readability; the file is the authority,
                // so an entry cannot claim to be somewhere it is not filed.
                category = category,
                aspect = obj.get("aspect")?.asFloat?.takeIf { it > 0f } ?: 1f,
                layers = layers
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping malformed lockup in ${category.name}", e)
            null
        }
    }

    private fun parseLayer(obj: JsonObject?): PresetLayer? {
        if (obj == null) return null
        val text = obj.get("text")?.asString ?: return null
        return PresetLayer(
            text = text,
            fontId = obj.get("fontId")?.asString,
            styleId = obj.get("styleId")?.asString,
            override = obj.getAsJsonObject("override"),
            // Fractions of the preset's own box, clamped: a layer authored outside the box
            // would insert off-canvas, which reads as the preset having done nothing.
            xPct = obj.get("xPct")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f,
            yPct = obj.get("yPct")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f,
            widthPct = obj.get("widthPct")?.asFloat?.coerceIn(0.05f, 1f) ?: 0.8f,
            rotation = obj.get("rotation")?.asFloat ?: 0f,
            align = obj.get("align")?.asString ?: "CENTER"
        )
    }
}
