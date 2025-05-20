package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.data.model.FontsResponse
import com.example.urduphotodesigner.domain.repo.FontsRepo
import javax.inject.Inject

class UpdateFontsUseCase @Inject constructor(
    private val fontsRepo: FontsRepo
) {
    suspend operator fun invoke(id: String, isDownloaded: Boolean, isDownloading: Boolean, filePath: String) {
        fontsRepo.updateFont(id, isDownloaded, isDownloading, filePath)
    }
}