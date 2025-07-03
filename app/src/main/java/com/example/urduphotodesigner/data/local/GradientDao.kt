package com.example.urduphotodesigner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urduphotodesigner.data.model.GradientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradientDao {
  @Query("SELECT * FROM gradient_presets")
  fun getAll(): Flow<List<GradientEntity>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(presets: List<GradientEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(entity: GradientEntity)

  @Query("DELETE FROM gradient_presets WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Update
  suspend fun updateGradient(entity: GradientEntity)

  @Query("SELECT COUNT(*) FROM gradient_presets")
  suspend fun count(): Int
}
