package com.example.urduphotodesigner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fonts")
data class FontEntity(
    @PrimaryKey val id: Int,
    val file_name: String,
    val font_name: String,
    val font_category: String,
    val file_url: String,
    val file_size: String,
    val font_image: String,
    val image_url: String,
    val alt_text: String,
    val user_id: Int,
    val created_at: String,
    val updated_at: String,
    var is_selected:Boolean = false
)
