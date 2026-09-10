package com.webscare.urducanvas.ui.editor.panels.text.symbols

import java.io.Serializable

/**
 * The four groups used to be Above / Below / "Marks" / "Ornaments", where
 * "Marks" was a bag holding Quranic stop signs, Islamic honorific ligatures and
 * two ordinary Urdu diacritics all at once. Each group is now one kind of thing,
 * so its rail label can say what is inside it.
 */
enum class SymbolCategory {
    UPPER,
    LOWER,
    QURANIC,
    HONORIFICS,
    ORNAMENTS
}

/**
 * [glyph] is what gets written into the element — always Arabic script, kept as
 * explicit \\u escapes because most of these are combining marks that are
 * invisible (and easy to mistype) as literal source characters.
 *
 * [name] is the caption under the tile. It follows the rest of the app chrome:
 * English/transliterated, never Urdu, to match Fonts / Presets / Spacing / Casing.
 */
data class SymbolItem(
    val id: String,
    val glyph: String,
    val name: String,
    val category: SymbolCategory,
    val isDiacritic: Boolean = true
) : Serializable

object SymbolsRepository {

    val UPPER_AIRAABS = listOf(
        SymbolItem("fatha", "َ", "Zabar", SymbolCategory.UPPER),
        SymbolItem("damma", "ُ", "Pesh", SymbolCategory.UPPER),
        SymbolItem("shaddah", "ّ", "Shaddah", SymbolCategory.UPPER),
        SymbolItem("fathatan", "ً", "Do Zabar", SymbolCategory.UPPER),
        SymbolItem("dammatan", "ٌ", "Do Pesh", SymbolCategory.UPPER),
        SymbolItem("khari_zabar", "ٰ", "Khari Zabar", SymbolCategory.UPPER),
        SymbolItem("maddah", "ٓ", "Maddah", SymbolCategory.UPPER),
        SymbolItem("hamza_above", "ٔ", "Hamza Above", SymbolCategory.UPPER),
        SymbolItem("sukun_round", "ْ", "Jazm", SymbolCategory.UPPER),
        SymbolItem("sukun_quranic", "ۡ", "Sukoon", SymbolCategory.UPPER),
        SymbolItem("shaddah_fatha", "َّ", "Shaddah Zabar", SymbolCategory.UPPER),
        SymbolItem("shaddah_damma", "ُّ", "Shaddah Pesh", SymbolCategory.UPPER),
        SymbolItem("shaddah_fathatan", "ًّ", "Shaddah Do Zabar", SymbolCategory.UPPER),
        SymbolItem("shaddah_dammatan", "\u0651\u064C", "Shaddah Do Pesh", SymbolCategory.UPPER),
        SymbolItem("shaddah_khari_zabar", "\u0651\u0670", "Shaddah Khari Zabar", SymbolCategory.UPPER),
        SymbolItem("wavy_hamza_above", "\u065A", "Wavy Hamza Above", SymbolCategory.UPPER),
        SymbolItem("small_high_seen", "\u06DC", "Small High Seen", SymbolCategory.UPPER),
        SymbolItem("small_high_meem", "\u06E2", "Small High Meem", SymbolCategory.UPPER),
        SymbolItem("small_high_noon", "\u06E8", "Small High Noon", SymbolCategory.UPPER),
        SymbolItem("small_high_yeh", "\u06E7", "Small High Yeh", SymbolCategory.UPPER),
        SymbolItem("high_madda", "\u06E4", "High Maddah", SymbolCategory.UPPER),
        SymbolItem("high_hamza", "\u0674", "High Hamza", SymbolCategory.UPPER),
        // Everyday Urdu diacritics that sit above a letter. They were filed
        // under the old "Marks" tab with the Quranic stop signs, where nobody
        // would think to look for them.
        SymbolItem("ulta_pesh", "\u0657", "Ulta Pesh", SymbolCategory.UPPER),
        SymbolItem("nun_ghunna", "\u0658", "Noon Ghunna", SymbolCategory.UPPER)
    )

    val LOWER_AIRAABS = listOf(
        SymbolItem("kasra", "ِ", "Zair", SymbolCategory.LOWER),
        SymbolItem("kasratan", "ٍ", "Do Zair", SymbolCategory.LOWER),
        SymbolItem("khari_zair", "ٖ", "Khari Zair", SymbolCategory.LOWER),
        SymbolItem("hamza_below", "ٕ", "Hamza Below", SymbolCategory.LOWER),
        SymbolItem("shaddah_kasra", "ِّ", "Shaddah Zair", SymbolCategory.LOWER),
        SymbolItem("shaddah_kasratan", "ٍّ", "Shaddah Do Zair", SymbolCategory.LOWER),
        SymbolItem("low_seen", "\u06E3", "Small Low Seen", SymbolCategory.LOWER),
        SymbolItem("low_meem", "\u06ED", "Small Low Meem", SymbolCategory.LOWER),
        SymbolItem("subscript_alef", "\u0656", "Subscript Alef", SymbolCategory.LOWER),
        SymbolItem("wavy_hamza_below", "\u065C", "Wavy Hamza Below", SymbolCategory.LOWER),
        SymbolItem("damma_below", "\u065F", "Pesh Below", SymbolCategory.LOWER)
    )

    /** Recitation and stop signs — everything that annotates Qur'anic text. */
    val QURANIC_MARKS = listOf(
        SymbolItem("ayah_stop", "۝", "Ayah End", SymbolCategory.QURANIC, isDiacritic = false),
        SymbolItem("waqf_lazim", "ۘ", "Waqf Lazim", SymbolCategory.QURANIC),
        SymbolItem("waqf_mutlaq", "ۚ", "Waqf Mutlaq", SymbolCategory.QURANIC),
        SymbolItem("waqf_jaiz", "ۖ", "Waqf Jaiz", SymbolCategory.QURANIC),
        SymbolItem("waqf_mujawwaz", "ۗ", "Waqf Mujawwaz", SymbolCategory.QURANIC),
        SymbolItem("waqf_murakhkhas", "ۛ", "Waqf Murakhkhas", SymbolCategory.QURANIC),
        SymbolItem("saktah", "ۜ", "Saktah", SymbolCategory.QURANIC),
        SymbolItem("rub_el_hizb", "۞", "Rub el Hizb", SymbolCategory.QURANIC, isDiacritic = false),
        SymbolItem("sajdah", "۩", "Sajdah", SymbolCategory.QURANIC, isDiacritic = false),

        // Waqf Qad Yusal — the one stop sign the original set left out.
        SymbolItem("waqf_qad_yusal", "ۙ", "Waqf Qad Yusal", SymbolCategory.QURANIC),

        // The small high marks. These carry recitation instructions rather than
        // vowels, so they belong here and not under Above.
        SymbolItem("high_rounded_zero", "۟", "Rounded Zero", SymbolCategory.QURANIC),
        SymbolItem("high_rect_zero", "۠", "Rectangular Zero", SymbolCategory.QURANIC),
        SymbolItem("high_sukun", "ۡ", "Sukun (Khah Head)", SymbolCategory.QURANIC),
        SymbolItem("high_meem", "ۢ", "High Meem", SymbolCategory.QURANIC),
        SymbolItem("high_madda", "ۤ", "High Madda", SymbolCategory.QURANIC),
        SymbolItem("high_yeh", "ۧ", "High Yeh", SymbolCategory.QURANIC),
        SymbolItem("high_noon", "ۨ", "High Noon", SymbolCategory.QURANIC),
        SymbolItem("low_meem", "ۭ", "Low Meem", SymbolCategory.QURANIC),

        // Sajdah and pause stops drawn as open or filled circles.
        SymbolItem("low_stop", "۪", "Empty Low Stop", SymbolCategory.QURANIC),
        SymbolItem("high_stop", "۫", "Empty High Stop", SymbolCategory.QURANIC),
        SymbolItem("high_stop_filled", "۬", "Filled High Stop", SymbolCategory.QURANIC),

        // Ornate parentheses, which is how an ayah number is normally set.
        SymbolItem("ornate_open", "﴾", "Ornate Bracket Open", SymbolCategory.QURANIC, isDiacritic = false),
        SymbolItem("ornate_close", "﴿", "Ornate Bracket Close", SymbolCategory.QURANIC, isDiacritic = false)
    )

    /** Names and salutations, each written as a single ligature glyph. */
    val HONORIFICS = listOf(
        SymbolItem("saw", "ﷺ", "Sallallahu", SymbolCategory.HONORIFICS, isDiacritic = false),
        SymbolItem("jj", "ﷻ", "Jalla Jalaluhu", SymbolCategory.HONORIFICS, isDiacritic = false),
        // U+0611 ALAYHE ASSALLAM, U+0612 RAHMATULLAH ALAYHE, U+0613 RADI ALLAHOU ANHU.
        // The first two used to be swapped against their captions.
        SymbolItem("as", "ؑ", "Alayhis Salam", SymbolCategory.HONORIFICS),
        SymbolItem("rh", "ؒ", "Rahmatullah", SymbolCategory.HONORIFICS),
        SymbolItem("ra", "ؓ", "Radiyallahu", SymbolCategory.HONORIFICS),
        SymbolItem("bismillah", "﷽", "Bismillah", SymbolCategory.HONORIFICS, isDiacritic = false),
        SymbolItem("allah", "ﷲ", "Allah", SymbolCategory.HONORIFICS, isDiacritic = false),
        SymbolItem("akbar", "ﷳ", "Akbar", SymbolCategory.HONORIFICS, isDiacritic = false),
        SymbolItem("muhammad", "ﷴ", "Muhammad", SymbolCategory.HONORIFICS, isDiacritic = false)
    )

    val ORNAMENTS = listOf(
        SymbolItem("dot_single", "•", "Single Dot", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("dot_double_h", "﮴", "Two Dots", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("dot_double_v", ":", "Two Dots Vertical", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("dot_triple", "⁂", "Three Dots", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("diamond_dot", "◆", "Diamond", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("hollow_diamond", "◇", "Hollow Diamond", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("four_corner", "❖", "Four Corner", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("arabic_star", "٭", "Star", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("flower", "❀", "Flower", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("crescent", "☽", "Crescent", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("flourish", "❦", "Flourish", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("heart", "♥", "Heart", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("kashida", "ـ", "Kashida", SymbolCategory.ORNAMENTS, isDiacritic = false),

        // Urdu poetry marks. These are the two an Urdu compositor actually reaches
        // for and neither was here: ؎ heads a couplet, ؏ marks a misra.
        SymbolItem("poetic_verse", "؎", "Verse Sign", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("misra", "؏", "Misra Sign", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("sanah", "؁", "Sanah (Year)", SymbolCategory.ORNAMENTS, isDiacritic = false),

        // Urdu punctuation, which is not what a Latin keyboard offers.
        SymbolItem("urdu_full_stop", "۔", "Full Stop", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("arabic_comma", "،", "Comma", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("arabic_semicolon", "؛", "Semicolon", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("arabic_question", "؟", "Question Mark", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("triple_dot_punct", "؞", "Triple Dot", SymbolCategory.ORNAMENTS, isDiacritic = false),

        // More florets and rules for framing a composition.
        SymbolItem("floret_six", "❋", "Six Floret", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("floret_eight", "✽", "Eight Floret", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("floret_open", "✾", "Open Floret", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("rosette", "✿", "Rosette", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("star_filled", "★", "Filled Star", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("star_hollow", "☆", "Hollow Star", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("lozenge", "◈", "Lozenge", SymbolCategory.ORNAMENTS, isDiacritic = false),
        SymbolItem("bullet_hollow", "◦", "Hollow Bullet", SymbolCategory.ORNAMENTS, isDiacritic = false)
    )

    fun getSymbolsForCategory(category: SymbolCategory): List<SymbolItem> {
        return when (category) {
            SymbolCategory.UPPER -> UPPER_AIRAABS
            SymbolCategory.LOWER -> LOWER_AIRAABS
            SymbolCategory.QURANIC -> QURANIC_MARKS
            SymbolCategory.HONORIFICS -> HONORIFICS
            SymbolCategory.ORNAMENTS -> ORNAMENTS
        }
    }
}
