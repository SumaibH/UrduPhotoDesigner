package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.domain.repo.FontsRepo
import javax.inject.Inject

class UpdateFontStatusUseCase @Inject constructor(
    private val fontsRepo: FontsRepo
) {

    suspend operator fun invoke(id: String, isDownloading: Boolean) {
        fontsRepo.updateStatusFont(id, isDownloading)
    }
}