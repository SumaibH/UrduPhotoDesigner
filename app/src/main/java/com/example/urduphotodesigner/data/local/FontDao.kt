package com.example.urduphotodesigner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.example.urduphotodesigner.data.model.FontEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FontDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFonts(fonts: FontEntity)

    @Query("SELECT * FROM fonts")
    fun getAllFonts(): Flow<List<FontEntity>>

    @Query("UPDATE fonts SET is_downloaded = :isDownloaded, is_downloading = :isDownloading, file_path = :filePath WHERE id = :id")
    fun updateFont(id: String, isDownloaded: Boolean, isDownloading: Boolean, filePath: String)

    @Query("UPDATE fonts SET is_downloading = :isDownloading WHERE id = :id")
    fun updateFontStatus(id: String, isDownloading: Boolean)

}
