package com.example.urduphotodesigner.common.utils

import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.toColorInt
import com.example.urduphotodesigner.common.canvas.model.ColorItem
import com.example.urduphotodesigner.common.canvas.model.GradientItem

object Constants {

    const val BASE_URL = "https://dashboard.urdufonts.com/api/"
    const val X_API_KEY = "21|kxJ7qhe4kjxjhfzQs4JWG34Pv8DeuIy0ZACTFe7Y5672dc67"
    const val BASE_URL_GLIDE = "https://dashboard.urdufonts.com/"
    var TEMPLATE = ""
    val colorList = listOf(
        "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF",
        "#FFFF00", "#00FFFF", "#FF00FF", "#C0C0C0", "#808080",
        "#800000", "#808000", "#008000", "#800080", "#008080",
        "#000080", "#FFA07A", "#FA8072", "#E9967A", "#F08080",
        "#CD5C5C", "#DC143C", "#B22222", "#8B0000", "#FF4500",
        "#FF6347", "#FF7F50", "#FF8C00", "#FFA500", "#FFD700",
        "#FFFFE0", "#FFFACD", "#FAFAD2", "#FFEFD5", "#FFE4B5",
        "#FFDAB9", "#EEE8AA", "#F0E68C", "#BDB76B", "#E6E6FA",
        "#D8BFD8", "#DDA0DD", "#EE82EE", "#DA70D6", "#FF00FF",
        "#BA55D3", "#9370DB", "#8A2BE2", "#9400D3", "#9932CC",
        "#8B008B", "#800080", "#4B0082", "#6A5ACD", "#483D8B",
        "#7B68EE", "#ADFF2F", "#7FFF00", "#7CFC00", "#00FF00",
        "#32CD32", "#98FB98", "#90EE90", "#00FA9A", "#00FF7F",
        "#3CB371", "#2E8B57", "#228B22", "#008000", "#006400",
        "#9ACD32", "#6B8E23", "#556B2F", "#66CDAA", "#8FBC8F",
        "#20B2AA", "#008B8B", "#008080", "#00FFFF", "#00CED1",
        "#40E0D0", "#48D1CC", "#00BFFF", "#1E90FF", "#6495ED",
        "#4682B4", "#4169E1", "#0000FF", "#0000CD", "#00008B",
        "#191970", "#87CEFA", "#87CEEB", "#ADD8E6", "#B0C4DE",
        "#708090", "#778899", "#A9A9A9", "#696969", "#2F4F4F"
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

    val glowColorList = listOf(
        "#FFFFFF",
        "#FFFF00",
        "#00FFFF",
        "#FF00FF",
        "#FF0000",
        "#00FF00",
        "#0000FF",
        "#FF6347",
        "#FFD700",
        "#FFFACD",
        "#FFE4B5",
        "#F0E68C",
        "#FF7F50",
        "#ADD8E6",
        "#FF4500",
        "#FF8C00",
        "#FA8072",
        "#FF1493",
        "#FFFFE0",
        "#32CD32",
        "#98FB98",
        "#FFDAB9"
    ).map { ColorItem(it) }

    val gradientList = listOf(
        GradientItem(
            listOf("#34495e".toColorInt(), "#2193b0".toColorInt(), "#45b8ac".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#3498db".toColorInt(),
                "#2ecc71".toColorInt(),
                "#f1c40f".toColorInt(),
                "#38ef7d".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#1abc9c".toColorInt(),
                "#e67e22".toColorInt(),
                "#00c6ff".toColorInt(),
                "#e15d44".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#009b77".toColorInt(),
                "#e67e22".toColorInt(),
                "#92a8d1".toColorInt(),
                "#11998e".toColorInt(),
                "#e15d44".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#f7cac9".toColorInt(),
                "#e74c3c".toColorInt(),
                "#88b04b".toColorInt(),
                "#f12711".toColorInt()
            ), GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf("#bc243c".toColorInt(), "#ff6f61".toColorInt(), "#88b04b".toColorInt()),
            GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#fad0c4".toColorInt(),
                "#2ecc71".toColorInt(),
                "#1abc9c".toColorInt(),
                "#f5af19".toColorInt(),
                "#2193b0".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#2ecc71".toColorInt(),
                "#34495e".toColorInt(),
                "#6dd5ed".toColorInt(),
                "#fbc2eb".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf("#ecf0f1".toColorInt(), "#e15d44".toColorInt(), "#2c3e50".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#009b77".toColorInt(),
                "#a18cd1".toColorInt(),
                "#bc243c".toColorInt(),
                "#27ae60".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#bdc3c7".toColorInt(), "#fbc2eb".toColorInt(), "#2ecc71".toColorInt()),
            GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf("#f39c12".toColorInt(), "#c0392b".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#6dd5ed".toColorInt(),
                "#e74c3c".toColorInt(),
                "#a18cd1".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf("#d35400".toColorInt(), "#6dd5ed".toColorInt(), "#bc243c".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#11998e".toColorInt(), "#bdc3c7".toColorInt(), "#9b59b6".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#ecf0f1".toColorInt(),
                "#f5af19".toColorInt(),
                "#ff6f61".toColorInt(),
                "#8e44ad".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#ff6f61".toColorInt(),
                "#f12711".toColorInt(),
                "#c0392b".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#27ae60".toColorInt(),
                "#009b77".toColorInt(),
                "#2c3e50".toColorInt(),
                "#955251".toColorInt(),
                "#bdc3c7".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf("#f5af19".toColorInt(), "#16a085".toColorInt(), "#7fcdcd".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#f1c40f".toColorInt(),
                "#0072ff".toColorInt(),
                "#11998e".toColorInt(),
                "#2ecc71".toColorInt(),
                "#92a8d1".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#00c6ff".toColorInt(), "#11998e".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#16a085".toColorInt(),
                "#fad0c4".toColorInt(),
                "#e67e22".toColorInt(),
                "#e74c3c".toColorInt(),
                "#7fcdcd".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#3498db".toColorInt(), "#6dd5ed".toColorInt(), "#FF5F6D".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#ff9a9e".toColorInt(),
                "#45b8ac".toColorInt(),
                "#00c6ff".toColorInt(),
                "#92a8d1".toColorInt()
            ), GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf("#1abc9c".toColorInt(), "#16a085".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#ff9a9e".toColorInt(),
                "#6b5b95".toColorInt(),
                "#c0392b".toColorInt(),
                "#ecf0f1".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf(
                "#f39c12".toColorInt(),
                "#7f8c8d".toColorInt(),
                "#3498db".toColorInt(),
                "#bc243c".toColorInt()
            ), GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#34495e".toColorInt(),
                "#f12711".toColorInt(),
                "#f1c40f".toColorInt(),
                "#1abc9c".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#bc243c".toColorInt(), "#38ef7d".toColorInt()),
            GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#9b59b6".toColorInt(), "#009b77".toColorInt(), "#d35400".toColorInt()),
            GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#0072ff".toColorInt(), "#8e44ad".toColorInt(), "#e74c3c".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#f5af19".toColorInt(),
                "#00c6ff".toColorInt(),
                "#6b5b95".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf("#6dd5ed".toColorInt(), "#fbc2eb".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf("#f7cac9".toColorInt(), "#f12711".toColorInt(), "#f1c40f".toColorInt()),
            GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#11998e".toColorInt(),
                "#3498db".toColorInt(),
                "#2193b0".toColorInt(),
                "#e67e22".toColorInt(),
                "#f7cac9".toColorInt()
            ), GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf("#f5af19".toColorInt(), "#45b8ac".toColorInt(), "#3498db".toColorInt()),
            GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf(
                "#fad0c4".toColorInt(),
                "#ecf0f1".toColorInt(),
                "#27ae60".toColorInt(),
                "#d65076".toColorInt(),
                "#38ef7d".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf(
                "#fad0c4".toColorInt(),
                "#c0392b".toColorInt(),
                "#FFC371".toColorInt(),
                "#8e44ad".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#bdc3c7".toColorInt(), "#b565a7".toColorInt(), "#f5af19".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf("#bdc3c7".toColorInt(), "#38ef7d".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#92a8d1".toColorInt(),
                "#38ef7d".toColorInt(),
                "#45b8ac".toColorInt(),
                "#a18cd1".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#00c6ff".toColorInt(),
                "#11998e".toColorInt(),
                "#45b8ac".toColorInt(),
                "#6b5b95".toColorInt(),
                "#3498db".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf(
                "#a18cd1".toColorInt(),
                "#009b77".toColorInt(),
                "#FF5F6D".toColorInt(),
                "#95a5a6".toColorInt(),
                "#ff9a9e".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#f1c40f".toColorInt(),
                "#00c6ff".toColorInt(),
                "#88b04b".toColorInt(),
                "#7f8c8d".toColorInt(),
                "#34495e".toColorInt()
            ), GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf(
                "#8e44ad".toColorInt(),
                "#45b8ac".toColorInt(),
                "#3498db".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf(
                "#16a085".toColorInt(),
                "#e15d44".toColorInt(),
                "#95a5a6".toColorInt(),
                "#f1c40f".toColorInt(),
                "#c0392b".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#ff6f61".toColorInt(), "#2193b0".toColorInt(), "#b565a7".toColorInt()),
            GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf(
                "#ecf0f1".toColorInt(),
                "#9b59b6".toColorInt(),
                "#FFC371".toColorInt(),
                "#955251".toColorInt(),
                "#16a085".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#45b8ac".toColorInt(), "#fad0c4".toColorInt(), "#ecf0f1".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#a18cd1".toColorInt(),
                "#6b5b95".toColorInt(),
                "#bdc3c7".toColorInt(),
                "#e74c3c".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#e67e22".toColorInt(), "#f1c40f".toColorInt()),
            GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#6b5b95".toColorInt(),
                "#7f8c8d".toColorInt(),
                "#38ef7d".toColorInt(),
                "#2193b0".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#009b77".toColorInt(), "#27ae60".toColorInt(), "#dd4124".toColorInt()),
            GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf(
                "#FFC371".toColorInt(),
                "#e74c3c".toColorInt(),
                "#1abc9c".toColorInt(),
                "#ff9a9e".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#9b59b6".toColorInt(),
                "#d65076".toColorInt(),
                "#ff6f61".toColorInt(),
                "#f1c40f".toColorInt(),
                "#f12711".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#88b04b".toColorInt(),
                "#6dd5ed".toColorInt(),
                "#e67e22".toColorInt(),
                "#2c3e50".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#f7cac9".toColorInt(), "#955251".toColorInt()),
            GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf("#f39c12".toColorInt(), "#fbc2eb".toColorInt(), "#38ef7d".toColorInt()),
            GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#45b8ac".toColorInt(), "#ecf0f1".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf("#95a5a6".toColorInt(), "#b565a7".toColorInt(), "#dd4124".toColorInt()),
            GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#e67e22".toColorInt(), "#009b77".toColorInt()),
            GradientDrawable.Orientation.LEFT_RIGHT
        ),
        GradientItem(
            listOf(
                "#6b5b95".toColorInt(),
                "#a18cd1".toColorInt(),
                "#92a8d1".toColorInt(),
                "#16a085".toColorInt()
            ), GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#FFC371".toColorInt(),
                "#bdc3c7".toColorInt(),
                "#d35400".toColorInt(),
                "#92a8d1".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#6b5b95".toColorInt(),
                "#7f8c8d".toColorInt(),
                "#88b04b".toColorInt(),
                "#1abc9c".toColorInt(),
                "#bc243c".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#2980b9".toColorInt(),
                "#e74c3c".toColorInt(),
                "#e67e22".toColorInt(),
                "#ff6f61".toColorInt(),
                "#f5af19".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#3498db".toColorInt(), "#955251".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#2980b9".toColorInt(),
                "#955251".toColorInt(),
                "#27ae60".toColorInt(),
                "#95a5a6".toColorInt(),
                "#FF5F6D".toColorInt()
            ), GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#ff9a9e".toColorInt(),
                "#FFC371".toColorInt(),
                "#fbc2eb".toColorInt(),
                "#a18cd1".toColorInt(),
                "#16a085".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#00c6ff".toColorInt(),
                "#16a085".toColorInt(),
                "#ecf0f1".toColorInt(),
                "#38ef7d".toColorInt()
            ), GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#bc243c".toColorInt(),
                "#27ae60".toColorInt(),
                "#dd4124".toColorInt(),
                "#f5af19".toColorInt(),
                "#f1c40f".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#FF5F6D".toColorInt(),
                "#00c6ff".toColorInt(),
                "#6b5b95".toColorInt(),
                "#2ecc71".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf(
                "#6dd5ed".toColorInt(),
                "#2980b9".toColorInt(),
                "#95a5a6".toColorInt(),
                "#6b5b95".toColorInt(),
                "#88b04b".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#e74c3c".toColorInt(), "#a18cd1".toColorInt()),
            GradientDrawable.Orientation.BL_TR
        ),
        GradientItem(
            listOf("#16a085".toColorInt(), "#3498db".toColorInt(), "#d35400".toColorInt()),
            GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#6dd5ed".toColorInt(),
                "#b565a7".toColorInt(),
                "#dd4124".toColorInt(),
                "#2ecc71".toColorInt(),
                "#a18cd1".toColorInt()
            ), GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf("#8e44ad".toColorInt(), "#11998e".toColorInt()),
            GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#ecf0f1".toColorInt(), "#e67e22".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#11998e".toColorInt(), "#1abc9c".toColorInt(), "#e15d44".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#1abc9c".toColorInt(), "#0072ff".toColorInt(), "#2193b0".toColorInt()),
            GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf(
                "#ecf0f1".toColorInt(),
                "#fad0c4".toColorInt(),
                "#c0392b".toColorInt(),
                "#009b77".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#7fcdcd".toColorInt(),
                "#b565a7".toColorInt(),
                "#d65076".toColorInt(),
                "#95a5a6".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#6b5b95".toColorInt(), "#6dd5ed".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf("#f39c12".toColorInt(), "#e15d44".toColorInt()),
            GradientDrawable.Orientation.RIGHT_LEFT
        ),
        GradientItem(
            listOf(
                "#27ae60".toColorInt(),
                "#2980b9".toColorInt(),
                "#b565a7".toColorInt(),
                "#95a5a6".toColorInt(),
                "#92a8d1".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#009b77".toColorInt(),
                "#6dd5ed".toColorInt(),
                "#f39c12".toColorInt(),
                "#1abc9c".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#45b8ac".toColorInt(),
                "#16a085".toColorInt(),
                "#88b04b".toColorInt(),
                "#2c3e50".toColorInt(),
                "#3498db".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#00c6ff".toColorInt(),
                "#fbc2eb".toColorInt(),
                "#a18cd1".toColorInt(),
                "#16a085".toColorInt()
            ), GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf("#38ef7d".toColorInt(), "#009b77".toColorInt(), "#95a5a6".toColorInt()),
            GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#f12711".toColorInt(), "#fbc2eb".toColorInt()),
            GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#e67e22".toColorInt(),
                "#d35400".toColorInt(),
                "#FFC371".toColorInt(),
                "#2ecc71".toColorInt(),
                "#34495e".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#88b04b".toColorInt(),
                "#fad0c4".toColorInt(),
                "#FF5F6D".toColorInt(),
                "#9b59b6".toColorInt(),
                "#92a8d1".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#2980b9".toColorInt(),
                "#fbc2eb".toColorInt(),
                "#92a8d1".toColorInt(),
                "#e15d44".toColorInt()
            ), GradientDrawable.Orientation.TOP_BOTTOM
        ),
        GradientItem(
            listOf(
                "#a18cd1".toColorInt(),
                "#2193b0".toColorInt(),
                "#9b59b6".toColorInt(),
                "#f5af19".toColorInt()
            ), GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#bc243c".toColorInt(), "#f1c40f".toColorInt()),
            GradientDrawable.Orientation.BOTTOM_TOP
        ),
        GradientItem(
            listOf(
                "#ff6f61".toColorInt(),
                "#3498db".toColorInt(),
                "#e15d44".toColorInt(),
                "#92a8d1".toColorInt(),
                "#fad0c4".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf("#8e44ad".toColorInt(), "#009b77".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf(
                "#e15d44".toColorInt(),
                "#e67e22".toColorInt(),
                "#34495e".toColorInt(),
                "#b565a7".toColorInt(),
                "#f12711".toColorInt()
            ), GradientDrawable.Orientation.TL_BR
        ),
        GradientItem(
            listOf("#e67e22".toColorInt(), "#b565a7".toColorInt()),
            GradientDrawable.Orientation.TR_BL
        ),
        GradientItem(
            listOf("#7fcdcd".toColorInt(), "#f12711".toColorInt()),
            GradientDrawable.Orientation.BR_TL
        ),
        GradientItem(
            listOf(
                "#16a085".toColorInt(),
                "#92a8d1".toColorInt(),
                "#bdc3c7".toColorInt(),
                "#00c6ff".toColorInt()
            ), GradientDrawable.Orientation.BR_TL
        )
    )
}