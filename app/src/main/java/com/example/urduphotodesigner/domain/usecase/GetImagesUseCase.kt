package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.data.model.ImageEntity
import com.example.urduphotodesigner.domain.repo.ImagesRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetImagesUseCase @Inject constructor(
    private val imagesRepo: ImagesRepo
) {
    operator fun invoke(): Flow<List<ImageEntity>> {
        return imagesRepo.fetchImages()
    }
}
