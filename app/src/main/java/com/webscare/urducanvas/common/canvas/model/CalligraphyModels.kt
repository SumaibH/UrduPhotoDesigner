package com.webscare.urducanvas.common.canvas.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

/**
 * Depth level when text is broken down for calligraphic composition.
 */
enum class ExpansionDepth {
    @SerializedName("Words")
    WORDS,

    @SerializedName("Characters")
    CHARACTERS,

    @SerializedName("Custom")
    CUSTOM
}

/**
 * Master container for an expanded/calligraphic text element.
 * When null on CanvasElement, standard linear text rendering is used.
 * When present, the element renders its tokens and floating accents.
 */
data class CalligraphyData(
    @SerializedName("mode") var expansionMode: ExpansionDepth = ExpansionDepth.WORDS,
    @SerializedName("tokens") var tokens: MutableList<TextToken> = mutableListOf(),
    @SerializedName("accents") var floatingAccents: MutableList<FloatingAccent> = mutableListOf(),
    @SerializedName("activeTokenId") var activeTokenId: String? = null,
    @SerializedName("originalFullText") var originalFullText: String = "",
    @SerializedName("isCompositionLocked") var isCompositionLocked: Boolean = false
) : Serializable {

    fun getActiveToken(): TextToken? {
        val id = activeTokenId ?: return null
        return tokens.firstOrNull { it.id == id }
    }

    fun findToken(id: String): TextToken? = tokens.firstOrNull { it.id == id }

    fun findAccent(id: String): FloatingAccent? = floatingAccents.firstOrNull { it.id == id }
}

/**
 * An individual word or character token within a calligraphic composition.
 */
data class TextToken(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("rawText") var rawText: String = "",
    @SerializedName("shapedText") var shapedText: String = "",
    @SerializedName("isDotless") var isDotless: Boolean = false,
    @SerializedName("diacritics") var diacritics: String = "",

    // Local coordinates relative to parent CanvasElement center
    @SerializedName("offsetX") var offsetX: Float = 0f,
    @SerializedName("offsetY") var offsetY: Float = 0f,
    @SerializedName("scale") var scale: Float = 1.0f,
    @SerializedName("rotation") var rotation: Float = 0f,
    @SerializedName("zIndex") var zIndex: Int = 0,

    // Tatweel/Kashida extension for this token
    @SerializedName("kashidaCount") var kashidaCount: Int = 0,

    // Overrides (null = inherit from parent CanvasElement)
    @SerializedName("overrideColor") var overrideColor: Int? = null,
    @SerializedName("overrideFontId") var overrideFontId: String? = null,
    @SerializedName("overrideGradient") var overrideGradient: GradientItem? = null,
    @SerializedName("overrideAlpha") var overrideAlpha: Int? = null,

    // Sequence index for lossless reconnection on collapse
    @SerializedName("orderIndex") val orderIndex: Int = 0
) : Serializable {

    /**
     * Complete display text including diacritics and kashida elongation.
     */
    fun getFullDisplayText(): String {
        val base = if (kashidaCount > 0) {
            val tatweel = "ـ".repeat(kashidaCount)
            // Insert tatweel before last char if possible or append
            if (shapedText.length > 1) {
                shapedText.substring(0, shapedText.length - 1) + tatweel + shapedText.last()
            } else {
                shapedText + tatweel
            }
        } else {
            shapedText
        }
        return base + diacritics
    }
}

/**
 * Free-floating decorative calligraphy accents (standalone dots, crescents, rosettes, flourishes).
 */
data class FloatingAccent(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("symbol") var symbol: String = "",
    @SerializedName("offsetX") var offsetX: Float = 0f,
    @SerializedName("offsetY") var offsetY: Float = 0f,
    @SerializedName("scale") var scale: Float = 1.0f,
    @SerializedName("rotation") var rotation: Float = 0f,
    @SerializedName("color") var color: Int? = null,
    @SerializedName("zIndex") var zIndex: Int = 0
) : Serializable
