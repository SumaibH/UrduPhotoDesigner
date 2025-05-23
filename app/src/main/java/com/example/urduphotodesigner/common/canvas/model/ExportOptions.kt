package com.example.urduphotodesigner.common.canvas.model

import android.graphics.Bitmap

data class ExportOptions(
    val resolution: ExportResolution,
    val quality: Int = 100, // New: 0-100 for compression quality
    val format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG // New: PNG or JPEG
)