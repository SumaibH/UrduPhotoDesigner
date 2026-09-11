package com.webscare.urducanvas.common.utils

import android.icu.lang.UCharacter
import com.webscare.urducanvas.common.canvas.model.ColorItem
import com.webscare.urducanvas.common.canvas.model.EmojiMeta
import java.util.Locale


object Constants {

    const val BASE_URL = "https://dashboard.urdufonts.com/api/"
    const val X_API_KEY = "21|kxJ7qhe4kjxjhfzQs4JWG34Pv8DeuIy0ZACTFe7Y5672dc67"
    const val BASE_URL_GLIDE = "https://dashboard.urdufonts.com/"
    const val BASE_URL_DOWNLOAD = "https://dashboard.urdufonts.com"

    // ── Tutorials ─────────────────────────────────────────────────────────────
    //
    // The Tutorials screen reads the channel's public Atom feed rather than the
    // YouTube Data API: no key to ship in the APK, no quota, no billing account.
    // The trade-off is that the feed only carries the 15 most recent uploads.
    //
    // The feed is addressed by channel id, not by the @handle. To re-derive it if the
    // channel ever moves, open https://www.youtube.com/@UrduCanvasEditor and read
    // "externalId":"UC…" out of the page source.
    const val YOUTUBE_CHANNEL_HANDLE = "@UrduCanvasEditor"
    const val YOUTUBE_CHANNEL_ID = "UChsxCI6I2g19AngjX0iflMg"
    const val YOUTUBE_FEED_URL =
        "https://www.youtube.com/feeds/videos.xml?channel_id=$YOUTUBE_CHANNEL_ID"
    const val YOUTUBE_CHANNEL_URL = "https://www.youtube.com/$YOUTUBE_CHANNEL_HANDLE"

    const val PEXELS_BASE_URL = "https://api.pexels.com/"
    const val PEXELS_API_KEY = "YUm6Jh5M8TEmXCi8UTIIZ0UbrByP5xWj8IFVoqkVy93mpihh4fQOYJxg"
    const val PEXELS_ID_OFFSET = 10_000_000
    const val GPU_SAFE_MAX_PX = 4899

    // ── Text spacing slider bounds ────────────────────────────────────────────
    //
    // Both spacings used to bottom out at -0.5, which is not a tight setting but a
    // broken one: line spacing multiplies the full line height, so 0.5 already stacks
    // descenders onto the next line's ascenders and anything below that inverts the
    // layout; letterSpacing is in ems, so -0.5 slides every glyph half its own width
    // into its neighbour. The floors below are the tightest values that still leave the
    // text readable in the Urdu faces the app ships.
    const val LINE_SPACING_MIN = 0.8f
    const val LINE_SPACING_MAX = 3.0f
    const val LETTER_SPACING_MIN = -0.05f
    const val LETTER_SPACING_MAX = 1.5f
    private val EMOTICONS = 0x1F600..0x1F64F
    private val SUPP_EMOTICONS = 0x1F910..0x1F91F
    private val ANIMAL_FACES = 0x1F400..0x1F43F
    private val WEATHER_NATURE = 0x1F300..0x1F32F
    private val FOOD_DRINK = 0x1F34F..0x1F37F
    private val SPORTS_LEISURE = 0x1F3A0..0x1F3FF
    private val TRANSPORT_MAP = 0x1F680..0x1F6FF
    private val MISC_OBJECTS = 0x1F4A0..0x1F4FF
    private val ALCHEMICAL_SYMBOLS = 0x1F700..0x1F77F
    private val GEOMETRIC_SHAPES = 0x1F780..0x1F7FF
    private val SUPP_ARROWS_C = 0x1F800..0x1F8FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val COUNTRY_CODES = listOf(
        "AF","AX","AL","DZ","AS","AD","AO","AI","AQ","AG",
        "AR","AM","AW","AU","AT","AZ","BS","BH","BD","BB",
        "BY","BE","BZ","BJ","BM","BT","BO","BQ","BA","BW",
        "BV","BR","IO","BN","BG","BF","BI","CV","KH","CM",
        "CA","KY","CF","TD","CL","CN","CX","CC","CO","KM",
        "CG","CD","CK","CR","CI","HR","CU","CW","CY","CZ",
        "DK","DJ","DM","DO","EC","EG","SV","GQ","ER","EE",
        "ET","FK","FO","FJ","FI","FR","GF","PF","TF","GA",
        "GM","GE","DE","GH","GI","GR","GL","GD","GP","GU",
        "GT","GG","GN","GW","GY","HT","HM","VA","HN","HK",
        "HU","IS","IN","ID","IR","IQ","IE","IM","IL","IT",
        "JM","JP","JE","JO","KZ","KE","KI","KP","KR","KW",
        "KG","LA","LV","LB","LS","LR","LY","LI","LT","LU",
        "MO","MK","MG","MW","MY","MV","ML","MT","MH","MQ",
        "MR","MU","YT","MX","FM","MD","MC","MN","ME","MS",
        "MA","MZ","MM","NA","NR","NP","NL","NC","NZ","NI",
        "NE","NG","NU","NF","MP","NO","OM","PK","PW","PS",
        "PA","PG","PY","PE","PH","PN","PL","PT","PR","QA",
        "RE","RO","RU","RW","BL","SH","KN","LC","MF","PM",
        "VC","WS","SM","ST","SA","SN","RS","SC","SL","SG",
        "SX","SK","SI","SB","SO","ZA","GS","SS","ES","LK",
        "SD","SR","SJ","SE","CH","SY","TW","TJ","TZ","TH",
        "TL","TG","TK","TO","TT","TN","TR","TM","TC","TV",
        "UG","UA","AE","GB","US","UM","UY","UZ","VU","VE",
        "VN","VG","VI","WF","EH","YE","ZM","ZW"
    )

    // 2. Helper to flatten any number of ranges into Strings
    private fun flatten(vararg ranges: IntRange): List<String> =
        ranges.flatMap { range ->
            range.mapNotNull { cp ->
                runCatching { String(Character.toChars(cp)) }.getOrNull()
            }
        }

    // 3. Public lists by category
    val EMOJI_EMOTICONS: List<String> by lazy { flatten(EMOTICONS, SUPP_EMOTICONS) }
    val EMOJI_ANIMALS: List<String> by lazy { flatten(ANIMAL_FACES) }
    val EMOJI_NATURE: List<String> by lazy { flatten(WEATHER_NATURE) }
    val EMOJI_FOOD: List<String> by lazy { flatten(FOOD_DRINK) }
    val EMOJI_SPORTS: List<String> by lazy { flatten(SPORTS_LEISURE) }
    val EMOJI_TRANSPORT: List<String> by lazy { flatten(TRANSPORT_MAP) }
    val EMOJI_OBJECTS: List<String> by lazy { flatten(MISC_OBJECTS) }
    val EMOJI_ALCHEMY: List<String> by lazy { flatten(ALCHEMICAL_SYMBOLS) }
    val EMOJI_SHAPES: List<String> by lazy { flatten(GEOMETRIC_SHAPES) }
    val EMOJI_ARROWS: List<String> by lazy { flatten(SUPP_ARROWS_C) }
    val EMOJI_FLAGS: List<String> by lazy { COUNTRY_CODES.map(::countryCodeToFlag) }
    val EMOJI_LETTERS: List<String> by lazy { flatten(REGIONAL_INDICATORS) }

    // 5. Build name-mapped lists per category
    private fun List<String>.withNames(): List<EmojiMeta> = mapNotNull { ch ->
        val cp = ch.codePointAt(0)
        runCatching {
            val raw = UCharacter.getName(cp) ?: return@mapNotNull null
            EmojiMeta(ch, raw.lowercase(Locale.ROOT).replace('_', ' '))
        }.getOrNull()
    }

    val META_EMOTICONS: List<EmojiMeta> by lazy { EMOJI_EMOTICONS.withNames() }
    val META_ANIMALS: List<EmojiMeta> by lazy { EMOJI_ANIMALS.withNames() }
    val META_NATURE: List<EmojiMeta> by lazy { EMOJI_NATURE.withNames() }
    val META_FOOD: List<EmojiMeta> by lazy { EMOJI_FOOD.withNames() }
    val META_SPORTS: List<EmojiMeta> by lazy { EMOJI_SPORTS.withNames() }
    val META_TRANSPORT: List<EmojiMeta> by lazy { EMOJI_TRANSPORT.withNames() }
    val META_OBJECTS: List<EmojiMeta> by lazy { EMOJI_OBJECTS.withNames() }
    val META_ALCHEMY: List<EmojiMeta> by lazy { EMOJI_ALCHEMY.withNames() }
    val META_SHAPES: List<EmojiMeta> by lazy { EMOJI_SHAPES.withNames() }
    val META_ARROWS: List<EmojiMeta> by lazy { EMOJI_ARROWS.withNames() }
    val META_FLAGS: List<EmojiMeta> by lazy {
        EMOJI_FLAGS.mapIndexed { i, flag ->
            val country = Locale("", COUNTRY_CODES[i]).displayCountry
            EmojiMeta(flag, "Flag of $country")
        }
    }
    val META_LETTERS: List<EmojiMeta> by lazy {
        EMOJI_LETTERS.mapNotNull { ch ->
            // getName("REGIONAL INDICATOR SYMBOL LETTER A") → "LETTER A"
            val raw = UCharacter.getName(ch.codePointAt(0)) ?: return@mapNotNull null
            val letter = raw.substringAfterLast(' ')    // "A"
            EmojiMeta(ch, letter)
        }
    }

    private fun countryCodeToFlag(code: String): String {
        val base = 0x1F1E6
        return code
            .uppercase(Locale.ROOT)
            .map { char ->
                String(Character.toChars(base + (char - 'A')))
            }
            .joinToString("")
    }

    /**
     * The swatch palette shared by every colour picker in the editor.
     *
     * Was 124 entries of which only 97 were distinct, opening on raw web primaries —
     * #FF0000, #00FF00, #0000FF — which are the colours nobody picks and everybody
     * recognises as a default. This is ordered instead: neutrals, then each hue as two
     * tints, the hue and two shades, then the palettes this app is actually used for.
     */
    val colorList = listOf(
        // Neutrals — white through black, warmed slightly so they sit on paper
        "#FFFFFF", "#F7F7F5", "#EDEDEA", "#DCDCD8", "#C4C4BF", "#A8A8A2",
        "#8A8A85", "#6B6B67", "#4E4E4B", "#333331", "#1C1C1B", "#000000",
        // Reds
        "#EEB0B0", "#E06D6D", "#D32F2F", "#9C2323", "#651717",
        // Crimson & maroon
        "#D3A9B0", "#AF606D", "#8C1C2E", "#681522", "#430D16",
        // Oranges
        "#FBCD9E", "#F8A34D", "#F57C00", "#B55C00", "#763C00",
        // Ambers & gold
        "#F5DBA8", "#ECBD5F", "#E4A11B", "#A97714", "#6D4D0D",
        // Yellows
        "#FAE89E", "#F6D44D", "#F2C200", "#B39000", "#745D00",
        // Limes
        "#D3E8BA", "#AED580", "#8BC34A", "#679037", "#435E24",
        // Greens
        "#B0CEB1", "#6DA470", "#2E7D32", "#225D25", "#163C18",
        // Emerald & flag green
        "#A2C7B4", "#549775", "#0B6B3A", "#084F2B", "#05331C",
        // Teals
        "#9ECCC7", "#4DA197", "#00796B", "#005A4F", "#003A33",
        // Cyans
        "#9ED7DE", "#4DB6C1", "#0097A7", "#00707C", "#004850",
        // Blues
        "#A6C4E7", "#5B93D3", "#1565C0", "#104B8E", "#0A305C",
        // Indigo & navy
        "#ADB2D6", "#6972B3", "#283593", "#1E276D", "#131947",
        // Violets
        "#C6B6E3", "#9779CB", "#6A3FB5", "#4E2F86", "#331E57",
        // Purples & plum
        "#CDAADC", "#A362BE", "#7B1FA2", "#5B1778", "#3B0F4E",
        // Magenta & rose
        "#E8A7C1", "#D45D8C", "#C2185B", "#901243", "#5D0C2C",
        // Pinks
        "#F7AAC4", "#F06292", "#E91E63", "#AC1649", "#700E30",
        // Browns & clay
        "#CCBEB9", "#A1887F", "#795548", "#5A3F35", "#3A2923",
        // Sand & cream
        "#FBF3E4", "#F1E2C3", "#E3CB9B", "#C8A86E", "#A2814A",
        // Ramadan & Eid — lantern gold on deep green
        "#03301C", "#1FA463", "#7FCBA3", "#E4C36A", "#F0D27A", "#FFF3C4",
        "#B98A2E",
        // Pakistan — the flag, and what sits well beside it
        "#01411C", "#04552A", "#D4AF37", "#123D2A",
        // Wedding — maroon, rose and gold
        "#4A0E1C", "#F0C1D4", "#D4A03C", "#5B1230",
        // Metals, as flat swatches
        "#FFD700", "#B08D57", "#C0C0C0", "#8E9BA5", "#B87333", "#6E7A82",
    ).map { ColorItem(it) }

    val shadowColorList = listOf(
        "#000000",
        "#808080",
        "#2F4F4F",
        "#4B0082",
        "#483D8B",
        "#6A5ACD",
        "#708090",
        "#8B0000",
        "#B22222",
        "#8B008B",
        "#556B2F",
        "#8FBC8F",
        "#00008B",
        "#191970",
        "#2E8B57",
        "#800000",
        "#A52A2A",
        "#D2691E",
        "#B22222",
        "#000080",
        "#2C3E50",
        "#3B3B3B",
        "#708090",
        "#4B0082"
    ).map { ColorItem(it) }
}