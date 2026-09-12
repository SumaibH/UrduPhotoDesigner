package com.webscare.urducanvas.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * The last few ids the user actually reached for, per panel shelf.
 *
 * Fonts already had a recents shelf, but it is derived in [com.webscare.urducanvas.viewmodels.MainViewModel]
 * from a Room table it joins against the font list — there is nothing to join for styles
 * and presets, whose catalogues are flat asset JSON. So this is the plainer thing: an id
 * list per shelf in SharedPreferences, newest first, written on every apply.
 *
 * Ids are stored rather than the styles themselves. A style the user applied last month
 * may have been renamed, retuned or deleted by a catalogue update since; resolving through
 * the repository on read means the shelf shows today's version of it, and an id that no
 * longer resolves simply drops out of the list instead of rendering something stale.
 */
object RecentsStore {

    /** Which shelf an id belongs to. The two lists never mix. */
    enum class Kind(internal val prefKey: String) {
        STYLE("recent_styles"),
        PRESET("recent_presets")
    }

    /**
     * How many ids a shelf keeps. Deep enough that a session's worth of styles stays
     * reachable, shallow enough that the shelf is still a shortcut rather than a
     * second catalogue to scroll.
     */
    const val MAX_ENTRIES = 30

    private const val PREF_NAME = "panel_recents"
    private val gson = Gson()

    private data class Entry(val id: String, val at: Long)

    /**
     * Records [id] as the most recently used on [kind]'s shelf.
     *
     * Re-applying something already on the shelf moves it to the front rather than
     * adding a second copy, so the list stays a set ordered by recency.
     */
    fun record(context: Context, kind: Kind, id: String) {
        if (id.isBlank()) return
        val current = read(context, kind).filterNot { it.id == id }
        val updated = (listOf(Entry(id, System.currentTimeMillis())) + current).take(MAX_ENTRIES)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(kind.prefKey, gson.toJson(updated))
            .apply()
    }

    /** The ids on [kind]'s shelf, newest first. */
    fun ids(context: Context, kind: Kind): List<String> = read(context, kind).map { it.id }

    /** Whether [kind]'s shelf has anything on it — the tab is hidden when it does not. */
    fun isEmpty(context: Context, kind: Kind): Boolean = read(context, kind).isEmpty()

    /** Forgets everything on [kind]'s shelf. */
    fun clear(context: Context, kind: Kind) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(kind.prefKey)
            .apply()
    }

    private fun read(context: Context, kind: Kind): List<Entry> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(kind.prefKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Entry>>() {}.type
            gson.fromJson<List<Entry>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            // A shelf that cannot be parsed is a shortcut the user loses, not a failure
            // worth propagating — start it over rather than taking the panel down with it.
            emptyList()
        }
    }
}
