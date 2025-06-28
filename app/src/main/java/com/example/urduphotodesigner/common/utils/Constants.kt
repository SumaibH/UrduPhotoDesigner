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

    val gradientList:List<GradientItem> = emptyList()
}