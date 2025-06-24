package com.example.urduphotodesigner.common.canvas.enums

enum class BlendType {
    SRC,
    DST,
    SRC_OVER,
    DST_OVER,
    SRC_IN,
    DST_IN,
    SRC_OUT,
    DST_OUT,
    SRC_ATOP,
    DST_ATOP,
    XOR,
    DARKEN,
    LIGHTEN,
    ADD,
    MULTIPLY,
    SCREEN
}
fun BlendType.displayName(): String = when (this) {
    BlendType.SRC        -> "Source"
    BlendType.DST        -> "Destination"
    BlendType.SRC_OVER   -> "Source Over"
    BlendType.DST_OVER   -> "Destination Over"
    BlendType.SRC_IN     -> "Source In"
    BlendType.DST_IN     -> "Destination In"
    BlendType.SRC_OUT    -> "Source Out"
    BlendType.DST_OUT    -> "Destination Out"
    BlendType.SRC_ATOP   -> "Source Atop"
    BlendType.DST_ATOP   -> "Destination Atop"
    BlendType.XOR        -> "XOR"
    BlendType.DARKEN     -> "Darken"
    BlendType.LIGHTEN    -> "Lighten"
    BlendType.ADD        -> "Add"
    BlendType.MULTIPLY   -> "Multiply"
    BlendType.SCREEN     -> "Screen"
}
