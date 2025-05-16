package com.example.urduphotodesigner.domain.repo

import com.example.urduphotodesigner.data.model.ImageEntity
import kotlinx.coroutines.flow.Flow

interface ImagesRepo {
    fun fetchImages(): Flow<List<ImageEntity>>
    suspend fun insertImages(imageEntity: ImageEntity)
}