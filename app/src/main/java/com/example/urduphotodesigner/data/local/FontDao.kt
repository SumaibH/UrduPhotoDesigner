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
}
