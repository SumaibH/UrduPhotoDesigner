package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.common.sealed.Response
import com.example.urduphotodesigner.data.model.ImageResponse
import com.example.urduphotodesigner.domain.repo.FetchImagesRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAPIImagesUseCase @Inject constructor(private val fetchImagesRepo: FetchImagesRepo) {
    operator fun invoke(): Flow<Response<ImageResponse>> {
        return fetchImagesRepo.fetchImages()
    }
}