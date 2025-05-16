package com.example.urduphotodesigner.data.repository

import com.example.urduphotodesigner.data.local.AppDatabase
import com.example.urduphotodesigner.data.model.ImageEntity
import com.example.urduphotodesigner.domain.repo.ImagesRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ImagesRepoImpl @Inject constructor(
    private val appDatabase: AppDatabase
) : ImagesRepo {

    override fun fetchImages(): Flow<List<ImageEntity>> {
        return appDatabase.imagesDao().getAllImages()
    }

    override suspend fun insertImages(imageEntity: ImageEntity) {
        appDatabase.imagesDao().insertImage(imageEntity)
    }
}

