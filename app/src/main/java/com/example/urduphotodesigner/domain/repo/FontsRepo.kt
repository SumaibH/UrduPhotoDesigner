package com.example.urduphotodesigner.domain.repo

import com.example.urduphotodesigner.data.model.FontEntity
import kotlinx.coroutines.flow.Flow

interface FontsRepo {
    fun fetchFonts(): Flow<List<FontEntity>>
    suspend fun insertFonts(fontEntity: FontEntity)
}