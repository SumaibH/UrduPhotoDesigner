package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.domain.repo.FontsRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFontsUseCase @Inject constructor(
    private val fontsRepo: FontsRepo
) {
    operator fun invoke(): Flow<List<FontEntity>> {
        return fontsRepo.fetchFonts()
    }
}
