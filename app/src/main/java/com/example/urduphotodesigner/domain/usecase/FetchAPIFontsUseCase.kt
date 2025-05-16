package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.common.Response
import com.example.urduphotodesigner.data.model.FontsResponse
import com.example.urduphotodesigner.domain.repo.FetchFontsRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAPIFontsUseCase @Inject constructor(private val fetchFontsRepo: FetchFontsRepo) {
    operator fun invoke(): Flow<Response<FontsResponse>> {
        return fetchFontsRepo.fetchFonts()
    }
}