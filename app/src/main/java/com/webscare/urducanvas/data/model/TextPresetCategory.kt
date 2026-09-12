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
 * alphabetically. Greetings leads because it is the only shelf with no season at all —
 * "صبح بخیر" is posted every morning of the year, where Ramadan is posted for thirty
 * days of it. The Islamic occasions follow, then the personal milestones, then the
 * commercial shelves, with Condolence last because nobody browses to it.
 *
 * The lowercased name is the asset file name (`presets/lockups/<name>.json`) and the
 * prefix every id in that file carries, which is what [TextPresetsRepository] guesses
 * with. No name may be a prefix of another followed by "_", or that guess goes to the
 * wrong file.
 *
 * Adding a category is additive and safe. Reordering is safe too — nothing persists an
 * ordinal; the Recents shelf stores preset ids. Renaming one is not: the file name and
 * every id in it move with it, and an id that moves reads to a user as a preset that
 * vanished out of their Recents.
 *
 * See `docs/preset-taxonomy.md` for what each shelf is for and why it earned one.
 */
enum class TextPresetCategory(val displayName: String) {
    GREETINGS("Greetings"),
    RAMADAN("Ramadan"),
    EID("Eid"),
    MILAD("Milad un Nabi"),
    ISLAMIC("Islamic"),
    DUA("Dua"),
    MUHARRAM("Muharram"),
    BIRTHDAY("Birthday"),
    WEDDING("Wedding"),
    CONGRATS("Congratulations"),
    LOVE("Love"),
    MOTIVATION("Motivation"),
    QUOTES("Quotes"),
    FAMILY("Family"),
    PAKISTAN("Pakistan"),
    BUSINESS("Business"),
    SALE("Sale"),
    FOOD("Food"),
    EDUCATION("Education"),
    SEASONS("Seasons"),
    TRAVEL("Travel"),
    HAJJ("Hajj & Umrah"),
    CONDOLENCE("Condolence");

    companion object {
        /** The category whose [displayName] matches [name], or null — tab titles are strings. */
        fun fromDisplayName(name: String?): TextPresetCategory? =
            values().firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
