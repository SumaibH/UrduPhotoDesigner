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

    /**
     * Independent copy, tokens and accents included.
     *
     * `CanvasElement.copy()` is a shallow data-class copy, so an undo snapshot
     * taken with it would share this very object — before and after would be
     * the same instance and undo would restore nothing.
     */
    fun deepCopy(): CalligraphyData = copy(
        tokens = tokens.map { it.copy() }.toMutableList(),
        floatingAccents = floatingAccents.map { it.copy() }.toMutableList()
    )

    /**
     * True once this composition holds anything that plain text cannot carry.
     *
     * Merging back to text keeps only what lives in the string — the letters,
     * their diacritics and dotless forms. A per-letter colour, font, outline,
     * shadow, kashida or arrangement has nowhere to go, so if any of those are
     * set the composition must survive rather than be flattened.
     */
    fun hasTokenEdits(): Boolean = floatingAccents.isNotEmpty() || tokens.any { token ->
        // Arrangement
        token.rotation != 0f ||
            token.scale != 1f ||
            kotlin.math.abs(token.offsetX - token.homeOffsetX) > 0.5f ||
            kotlin.math.abs(token.offsetY - token.homeOffsetY) > 0.5f ||
            // Per-letter styling — none of this survives a collapse
            token.overrideColor != null ||
            token.overrideGradient != null ||
            token.overrideAlpha != null ||
            token.overrideFontId != null ||
            token.overrideHasStroke != null ||
            token.overrideStrokeColor != null ||
            token.overrideStrokeWidth != null ||
            token.overrideHasShadow != null ||
            token.overrideShadowColor != null ||
            token.overrideShadowRadius != null ||
            token.kashidaCount > 0
    }


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

    /**
     * Font file backing [overrideFontId]. CanvasView has no font catalogue of
     * its own, so the path travels with the token — otherwise a per-token font
     * could be stored but never drawn.
     */
    @SerializedName("overrideFontPath") var overrideFontPath: String? = null,

    // Per-letter outline and shadow. The renderer already draws both per token,
    // it simply read the element's values; null still means "inherit".
    @SerializedName("overrideHasStroke") var overrideHasStroke: Boolean? = null,
    @SerializedName("overrideStrokeColor") var overrideStrokeColor: Int? = null,
    @SerializedName("overrideStrokeWidth") var overrideStrokeWidth: Float? = null,
    @SerializedName("overrideHasShadow") var overrideHasShadow: Boolean? = null,
    @SerializedName("overrideShadowColor") var overrideShadowColor: Int? = null,
    @SerializedName("overrideShadowRadius") var overrideShadowRadius: Float? = null,
    @SerializedName("overrideShadowDx") var overrideShadowDx: Float? = null,
    @SerializedName("overrideShadowDy") var overrideShadowDy: Float? = null,

    /**
     * Whitespace that preceded this token in the original text — usually "" or
     * " ", but a newline survives here too.
     *
     * Decomposition used to split on `\s+` and discard the separators, so
     * collapsing a character-level composition welded the whole line into one
     * word. Only the first token of each word carries a non-empty value.
     */
    @SerializedName("separatorBefore") var separatorBefore: String = "",

    /**
     * Where this token was placed by the natural layout. Kept so "has the user
     * actually arranged anything?" can be answered — offsets are non-zero from
     * birth, so a plain zero check would never be true.
     */
    @SerializedName("homeOffsetX") var homeOffsetX: Float = 0f,
    @SerializedName("homeOffsetY") var homeOffsetY: Float = 0f,

    // Sequence index for lossless reconnection on collapse
    @SerializedName("orderIndex") val orderIndex: Int = 0
) : Serializable {

    /** Resolved [overrideFontPath], cached per token. Never serialized. */
    @Transient
    private var cachedTypeface: android.graphics.Typeface? = null

    @Transient
    private var cachedTypefacePath: String? = null

    /** Typeface for this token, or null to inherit the element's. */
    fun resolveTypeface(): android.graphics.Typeface? {
        val path = overrideFontPath ?: return null
        if (cachedTypeface != null && cachedTypefacePath == path) return cachedTypeface
        cachedTypeface = try {
            android.graphics.Typeface.createFromFile(path)
        } catch (e: Exception) {
            null
        }
        cachedTypefacePath = path
        return cachedTypeface
    }

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
