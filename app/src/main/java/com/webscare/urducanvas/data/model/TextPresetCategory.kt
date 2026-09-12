package com.webscare.urducanvas.data.model

/**
 * The occasions the preset shelf is organised by.
 *
 * Separate from [PresetCategory], which names the *style* catalogue's thirteen visual
 * treatments (3D, Emboss, Neon …). A style paints one text element; a preset is a whole
 * lockup of two or three phrases, and what a user browses it by is the occasion they are
 * making a post for, not the effect it happens to wear.
 *
 * Declaration order is shelf order, and it runs by expected demand rather than
 * alphabetically — Ramadan and Eid lead because they carry the heaviest content weight.
 */
enum class TextPresetCategory(val displayName: String) {
    RAMADAN("Ramadan"),
    EID("Eid"),
    ISLAMIC("Islamic"),
    WEDDING("Wedding"),
    BIRTHDAY("Birthday"),
    PAKISTAN("Pakistan"),
    BUSINESS("Business"),
    QUOTES("Quotes"),
    SALE("Sale"),
    CONDOLENCE("Condolence");

    companion object {
        /** The category whose [displayName] matches [name], or null — tab titles are strings. */
        fun fromDisplayName(name: String?): TextPresetCategory? =
            values().firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
