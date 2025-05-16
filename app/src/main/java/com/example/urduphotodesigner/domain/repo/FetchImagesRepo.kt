package com.example.urduphotodesigner.domain.repo

import com.example.urduphotodesigner.common.Response
import com.example.urduphotodesigner.data.model.ImageResponse
import kotlinx.coroutines.flow.Flow

interface FetchImagesRepo {
    fun fetchImages(): Flow<Response<ImageResponse>>
}