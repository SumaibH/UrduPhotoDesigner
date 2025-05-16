package com.example.urduphotodesigner.domain.repo

import com.example.urduphotodesigner.common.Response
import com.example.urduphotodesigner.data.model.FontsResponse
import kotlinx.coroutines.flow.Flow

interface FetchFontsRepo {
    fun fetchFonts(): Flow<Response<FontsResponse>>
}