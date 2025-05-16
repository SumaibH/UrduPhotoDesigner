package com.example.urduphotodesigner.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.data.model.ImageEntity

@androidx.room.Database(entities = [FontEntity::class, ImageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fontsDao(): FontDao
    abstract fun imagesDao(): ImageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "UrduPhotoDesigner.db"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }
}