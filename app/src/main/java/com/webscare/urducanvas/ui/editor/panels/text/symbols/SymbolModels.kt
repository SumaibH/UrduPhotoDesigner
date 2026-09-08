package com.webscare.urducanvas.ui.editor.panels.text.symbols

import java.io.Serializable

enum class SymbolCategory {
    UPPER,
    LOWER,
    SIDE_QURANIC,
    DOTS_ACCENTS
}

data class SymbolItem(
    val id: String,
    val glyph: String,
    val name: String,
    val category: SymbolCategory,
    val isDiacritic: Boolean = true
) : Serializable

object SymbolsRepository {

    val UPPER_AIRAABS = listOf(
        SymbolItem("fatha", "\u064E", "زبر (Fatha)", SymbolCategory.UPPER),
        SymbolItem("damma", "\u064F", "پیش (Damma)", SymbolCategory.UPPER),
        SymbolItem("shaddah", "\u0651", "تشدید (Shaddah)", SymbolCategory.UPPER),
        SymbolItem("fathatan", "\u064B", "دو زبر (Tanween)", SymbolCategory.UPPER),
        SymbolItem("dammatan", "\u064C", "دو پیش", SymbolCategory.UPPER),
        SymbolItem("khari_zabar", "\u0670", "کھڑی زبر", SymbolCategory.UPPER),
        SymbolItem("maddah", "\u0653", "مَد (Maddah)", SymbolCategory.UPPER),
        SymbolItem("hamza_above", "\u0654", "ہمزہ اوپر", SymbolCategory.UPPER),
        SymbolItem("sukun_round", "\u0652", "جزم / سکون", SymbolCategory.UPPER),
        SymbolItem("sukun_quranic", "\u06E1", "سکون قرآنی", SymbolCategory.UPPER),
        SymbolItem("shaddah_fatha", "\u0651\u064E", "تشدید + زبر", SymbolCategory.UPPER),
        SymbolItem("shaddah_damma", "\u0651\u064F", "تشدید + پیش", SymbolCategory.UPPER),
        SymbolItem("shaddah_fathatan", "\u0651\u064B", "تشدید + دو زبر", SymbolCategory.UPPER)
    )

    val LOWER_AIRAABS = listOf(
        SymbolItem("kasra", "\u0650", "زیر (Kasra)", SymbolCategory.LOWER),
        SymbolItem("kasratan", "\u064D", "دو زیر", SymbolCategory.LOWER),
        SymbolItem("khari_zair", "\u0656", "کھڑی زیر", SymbolCategory.LOWER),
        SymbolItem("hamza_below", "\u0655", "ہمزہ نیچے", SymbolCategory.LOWER),
        SymbolItem("shaddah_kasra", "\u0651\u0650", "تشدید + زیر", SymbolCategory.LOWER),
        SymbolItem("shaddah_kasratan", "\u0651\u064D", "تشدید + دو زیر", SymbolCategory.LOWER)
    )

    val SIDE_QURANIC_SYMBOLS = listOf(
        SymbolItem("ulta_pesh", "\u0657", "الٹا پیش", SymbolCategory.SIDE_QURANIC),
        SymbolItem("nun_ghunna", "\u0658", "نون غنہ", SymbolCategory.SIDE_QURANIC),
        SymbolItem("ayah_stop", "\u06DD", "آیت ختم ۝", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("waqf_lazim", "\u06D8", "وقف لازم (مـ)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("waqf_mutlaq", "\u06DA", "وقف مطلق (ط)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("waqf_jaiz", "\u06D6", "وقف جائز (ج)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("waqf_mujawwaz", "\u06D7", "وقف مجوز (ز)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("waqf_murakhkhas", "\u06DB", "وقف مرخص (ص)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("saktah", "\u06DC", "سکتہ", SymbolCategory.SIDE_QURANIC),
        SymbolItem("rub_el_hizb", "\u06DE", "حزب ۞", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("sajdah", "\u06E9", "سجدہ ۩", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("saw", "\uFDFA", "ﷺ", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("jj", "\uFDFB", "ﷻ", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("as", "\u0612", "ؑ (علیہ السلام)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("ra", "\u0613", "ؓ (رضی اللہ عنہ)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("rh", "\u0611", "ؒ (رحمۃ اللہ)", SymbolCategory.SIDE_QURANIC),
        SymbolItem("bismillah", "\uFDFD", "﷽", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("allah", "\uFDF2", "ﷲ", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("akbar", "\uFDF3", "اکبر", SymbolCategory.SIDE_QURANIC, isDiacritic = false),
        SymbolItem("muhammad", "\uFDF4", "محمد", SymbolCategory.SIDE_QURANIC, isDiacritic = false)
    )

    val DOTS_ACCENTS = listOf(
        SymbolItem("dot_single", "\u2022", "ایک نقطہ •", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("dot_double_h", "\uFBB4", "دو نقطے (افقی)", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("dot_double_v", ":", "دو نقطے (عمودی)", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("dot_triple", "\u2042", "تین نقطے ⁂", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("diamond_dot", "\u25C6", "ہیرا نقطہ ◆", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("hollow_diamond", "\u25C7", "خالی ہیرا ◇", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("four_corner", "\u2756", "چار گوشہ ❖", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("arabic_star", "\u066D", "ستارہ ٭", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("flower", "\u2740", "پھول ❀", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("crescent", "\u263D", "ہلال ☽", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("flourish", "\u2766", "خوش خط ❦", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("heart", "\u2665", "دل ♥", SymbolCategory.DOTS_ACCENTS, isDiacritic = false),
        SymbolItem("kashida", "\u0640", "کشیدہ ـ", SymbolCategory.DOTS_ACCENTS, isDiacritic = false)
    )

    fun getSymbolsForCategory(category: SymbolCategory): List<SymbolItem> {
        return when (category) {
            SymbolCategory.UPPER -> UPPER_AIRAABS
            SymbolCategory.LOWER -> LOWER_AIRAABS
            SymbolCategory.SIDE_QURANIC -> SIDE_QURANIC_SYMBOLS
            SymbolCategory.DOTS_ACCENTS -> DOTS_ACCENTS
        }
    }
}
