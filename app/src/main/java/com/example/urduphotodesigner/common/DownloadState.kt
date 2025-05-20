package com.example.urduphotodesigner.common

import android.graphics.Typeface
import java.io.File

sealed class DownloadState {
    data class Progress(val progress: Int) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
    data class SuccessWithTypeface(val file: File, val typeface: Typeface) : DownloadState()
}