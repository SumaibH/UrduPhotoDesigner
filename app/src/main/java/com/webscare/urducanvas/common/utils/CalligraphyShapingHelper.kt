package com.webscare.urducanvas.common.utils

import android.graphics.Paint
import com.webscare.urducanvas.common.canvas.model.CalligraphyData
import com.webscare.urducanvas.common.canvas.model.ExpansionDepth
import com.webscare.urducanvas.common.canvas.model.TextToken

object CalligraphyShapingHelper {

    const val ZWJ = "\u200D"

    // Letters that do NOT connect to the subsequent (following) letter (Right-joining only)
    private val RIGHT_JOINING_ONLY = setOf(
        'ا', 'آ', 'أ', 'إ', 'ٱ',
        'د', 'ڈ', 'ذ', 'ډ', 'ڊ', 'ڌ',
        'ر', 'ڑ', 'ز', 'ژ', 'ړ', 'ږ', 'ڙ',
        'و', 'ؤ', 'ۄ', 'ۅ', 'ۆ', 'ۇ', 'ۈ', 'ۉ', 'ۋ',
        'ے'
    )

    // Protected Ligatures that should stay together as a single token during character split
    private val PROTECTED_LIGATURES = listOf(
        "الله", "اللہ", "لا", "لآ", "لأ", "لإ", "لٱ"
    )

    // Dotless skeleton map (Dotted Arabic/Urdu letters -> Dotless base skeletons)
    private val DOTTED_TO_DOTLESS_MAP = mapOf(
        'ب' to 'ٮ', 'پ' to 'ٮ', 'ت' to 'ٮ', 'ٹ' to 'ٮ', 'ث' to 'ٮ',
        'ج' to 'ح', 'چ' to 'ح', 'خ' to 'ح',
        'ڈ' to 'د', 'ذ' to 'د',
        'ڑ' to 'ر', 'ز' to 'ر', 'ژ' to 'ر',
        'ش' to 'س',
        'ض' to 'ص',
        'ظ' to 'ط',
        'غ' to 'ع',
        'ف' to 'ڡ',
        'ق' to 'ٯ',
        'ن' to 'ں',
        'ی' to 'ى'
    )

    /**
     * Checks if a character is an Arabic/Urdu combining diacritic (I'raab / Tashkeel).
     */
    fun isDiacritic(c: Char): Boolean {
        val code = c.code
        return (code in 0x064B..0x065F) || (code == 0x0670) || (code in 0x0610..0x061A) || (code in 0x06D6..0x06ED)
    }

    /**
     * Removes all combining diacritics from a string.
     */
    fun stripDiacritics(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (!isDiacritic(ch)) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Checks if character connects to following letters (dual-joining).
     */
    private fun canConnectForward(c: Char): Boolean {
        if (!isArabicLetter(c)) return false
        return !RIGHT_JOINING_ONLY.contains(c)
    }

    /**
     * Checks if a character is an Arabic/Urdu base letter.
     */
    fun isArabicLetter(c: Char): Boolean {
        val code = c.code
        return (code in 0x0621..0x064A) || (code in 0x0671..0x06D3) || (code in 0x06EE..0x06FF)
    }

    /**
     * Converts a letter to its dotless calligraphy skeleton.
     */
    fun toDotlessSkeleton(c: Char): Char {
        return DOTTED_TO_DOTLESS_MAP[c] ?: c
    }

    /**
     * Breaks an input string into [TextToken]s based on requested [ExpansionDepth].
     * Computes initial natural reading offsets using the provided [paint].
     */
    fun decomposeText(
        text: String,
        depth: ExpansionDepth,
        paint: Paint
    ): CalligraphyData {
        val tokens = mutableListOf<TextToken>()
        var orderIndex = 0

        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return CalligraphyData(
                expansionMode = depth,
                tokens = tokens,
                originalFullText = text
            )
        }

        when (depth) {
            ExpansionDepth.WORDS -> {
                // Split by spaces, preserving punctuation
                val words = cleanText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                for (word in words) {
                    tokens.add(
                        TextToken(
                            rawText = word,
                            shapedText = word,
                            orderIndex = orderIndex++
                        )
                    )
                }
            }

            ExpansionDepth.CHARACTERS, ExpansionDepth.CUSTOM -> {
                // Character-level decomposition with protected ligatures and contextual ZWJ
                val words = cleanText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                for (word in words) {
                    val wordTokens = decomposeWordIntoLetters(word, orderIndex)
                    tokens.addAll(wordTokens)
                    orderIndex += wordTokens.size
                }
            }
        }

        // Calculate natural RTL layout offsets so tokens start in their natural positions
        calculateInitialTokenLayout(tokens, paint)

        return CalligraphyData(
            expansionMode = depth,
            tokens = tokens,
            activeTokenId = tokens.firstOrNull()?.id,
            originalFullText = text
        )
    }

    /**
     * Decomposes a single word into contextual letters with ZWJ shaping and protected ligatures.
     */
    private fun decomposeWordIntoLetters(word: String, startOrderIndex: Int): List<TextToken> {
        val result = mutableListOf<TextToken>()
        var i = 0
        var currentIndex = startOrderIndex

        while (i < word.length) {
            // 1. Check for protected ligatures
            var matchedLigature: String? = null
            for (lig in PROTECTED_LIGATURES) {
                if (word.startsWith(lig, i)) {
                    matchedLigature = lig
                    break
                }
            }

            if (matchedLigature != null) {
                result.add(
                    TextToken(
                        rawText = matchedLigature,
                        shapedText = matchedLigature,
                        orderIndex = currentIndex++
                    )
                )
                i += matchedLigature.length
                continue
            }

            val ch = word[i]
            if (isDiacritic(ch)) {
                // If there's an orphaned diacritic, attach to previous token if available
                if (result.isNotEmpty()) {
                    result.last().diacritics += ch
                }
                i++
                continue
            }

            // Extract base character + any attached diacritics
            val rawChar = ch.toString()
            var diacritics = ""
            var nextIndex = i + 1
            while (nextIndex < word.length && isDiacritic(word[nextIndex])) {
                diacritics += word[nextIndex]
                nextIndex++
            }

            // Determine contextual shaping (Initial, Medial, Final, Isolated)
            val prevChar = if (i > 0) word[i - 1] else null
            val nextChar = if (nextIndex < word.length) word[nextIndex] else null

            val connectsBackward = prevChar != null && canConnectForward(prevChar)
            val connectsForward = nextChar != null && canConnectForward(ch) && isArabicLetter(nextChar)

            val shaped = when {
                connectsBackward && connectsForward -> "$ZWJ$rawChar$ZWJ" // Medial
                connectsForward -> "$rawChar$ZWJ"                         // Initial
                connectsBackward -> "$ZWJ$rawChar"                        // Final
                else -> rawChar                                           // Isolated
            }

            result.add(
                TextToken(
                    rawText = rawChar,
                    shapedText = shaped,
                    diacritics = diacritics,
                    orderIndex = currentIndex++
                )
            )

            i = nextIndex
        }

        return result
    }

    /**
     * Arranges tokens horizontally in RTL order centered around (0, 0).
     */
    private fun calculateInitialTokenLayout(tokens: List<TextToken>, paint: Paint) {
        if (tokens.isEmpty()) return

        val spaceWidth = paint.measureText(" ")
        val widths = tokens.map { paint.measureText(it.getFullDisplayText()) }
        val totalWidth = widths.sum() + (tokens.size - 1) * spaceWidth

        // In RTL: first token starts on the right (+totalWidth / 2) and flows left
        var currentRight = totalWidth / 2f
        for (i in tokens.indices) {
            val token = tokens[i]
            val w = widths[i]
            token.offsetX = currentRight - w / 2f
            token.offsetY = 0f
            token.scale = 1.0f
            token.rotation = 0f
            token.zIndex = i
            currentRight -= (w + spaceWidth)
        }
    }

    /**
     * Toggles a token between its normal letter and its dotless skeleton form.
     */
    fun toggleDotless(token: TextToken) {
        if (token.rawText.isEmpty()) return
        val firstChar = token.rawText.first()

        if (!token.isDotless) {
            val dotless = toDotlessSkeleton(firstChar)
            if (dotless != firstChar) {
                token.isDotless = true
                val newRaw = dotless.toString() + token.rawText.substring(1)
                token.shapedText = token.shapedText.replace(firstChar, dotless)
            }
        } else {
            // Restore original letter from rawText / shapedText
            token.isDotless = false
            // If rawText matches a skeleton, restore the primary letter
            token.shapedText = token.shapedText.replace('ٮ', 'ب')
                .replace('ڡ', 'ف')
                .replace('ٯ', 'ق')
                .replace('ں', 'ن')
                .replace('ى', 'ی')
        }
    }

    /**
     * Collapses all tokens back into a single clean Urdu/Arabic string.
     * Strips all ZWJ characters and rejoins with proper spacing.
     */
    fun collapseToString(calligraphyData: CalligraphyData): String {
        val sorted = calligraphyData.tokens.sortedBy { it.orderIndex }
        if (sorted.isEmpty()) return calligraphyData.originalFullText

        return when (calligraphyData.expansionMode) {
            ExpansionDepth.WORDS -> {
                sorted.joinToString(" ") { token ->
                    stripZwj(token.rawText) + token.diacritics
                }
            }
            ExpansionDepth.CHARACTERS, ExpansionDepth.CUSTOM -> {
                val sb = StringBuilder()
                for (token in sorted) {
                    sb.append(stripZwj(token.rawText))
                    sb.append(token.diacritics)
                }
                sb.toString()
            }
        }
    }

    fun stripZwj(str: String): String = str.replace(ZWJ, "")
}
