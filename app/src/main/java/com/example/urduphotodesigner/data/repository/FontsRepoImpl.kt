package com.example.urduphotodesigner.data.repository

import com.example.urduphotodesigner.data.local.AppDatabase
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.domain.repo.FontsRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FontsRepoImpl @Inject constructor(
    private val appDatabase: AppDatabase
) : FontsRepo {

    override fun fetchFonts(): Flow<List<FontEntity>> {
        return appDatabase.fontsDao().getAllFonts()
    }

    override suspend fun insertFonts(fontEntity: FontEntity) {
        appDatabase.fontsDao().insertFonts(fontEntity)
    }
}

