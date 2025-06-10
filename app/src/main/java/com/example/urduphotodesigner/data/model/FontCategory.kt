package com.example.urduphotodesigner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "font_category")
data class FontCategory(
    @PrimaryKey val id: Int,
    val font_category: String,
    var is_selected:Boolean = false
)
