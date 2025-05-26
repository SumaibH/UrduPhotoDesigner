package com.example.urduphotodesigner.common.canvas.sealed

sealed class ImageFilter {
    data object None : ImageFilter()
    data object Grayscale : ImageFilter()
    data object Sepia : ImageFilter()
    data object Invert : ImageFilter()
    data object CoolTint : ImageFilter()
    data object WarmTint : ImageFilter()
    data object Vintage : ImageFilter()
    data object Film : ImageFilter()           // New
    data object TealOrange : ImageFilter()     // New
    data object HighContrast : ImageFilter()   // New
    data object BlackWhite : ImageFilter()     // New
}