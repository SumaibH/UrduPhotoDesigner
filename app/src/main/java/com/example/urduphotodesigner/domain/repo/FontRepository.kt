package com.example.urduphotodesigner.domain.repo

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class FontRepository @Inject constructor(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val fontsDir by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fonts").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun downloadFont(
        fontUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(fontsDir, fileName)
        
        val request = Request.Builder()
            .url(fontUrl)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download font: ${response.code}")
            }

            val contentLength = response.body?.contentLength() ?: 0L
            var bytesDownloaded = 0L
            var lastProgress = 0

            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        // Calculate and update progress
                        if (contentLength > 0) {
                            val progress = ((bytesDownloaded * 100) / contentLength).toInt()
                            if (progress > lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
        }
        return@withContext outputFile
    }

    fun getLocalFontFile(fileName: String): File? {
        val file = File(fontsDir, fileName)
        return if (file.exists()) file else null
    }
}