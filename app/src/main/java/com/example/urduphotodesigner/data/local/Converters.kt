package com.example.urduphotodesigner.data.local

import androidx.room.TypeConverter
import com.example.urduphotodesigner.common.canvas.enums.GradientType

object Converters {
  @TypeConverter
  @JvmStatic
  fun fromIntList(list: List<Int>?): String =
    list?.joinToString(",") ?: ""

  @TypeConverter
  @JvmStatic
  fun toIntList(data: String?): List<Int> =
    data
      ?.takeIf { it.isNotEmpty() }
      ?.split(",")
      ?.map { it.toInt() }
      ?: emptyList()

  @TypeConverter
  @JvmStatic
  fun fromFloatList(list: List<Float>?): String =
    list?.joinToString(",") ?: ""

  @TypeConverter
  @JvmStatic
  fun toFloatList(data: String?): List<Float> =
    data
      ?.takeIf { it.isNotEmpty() }
      ?.split(",")
      ?.map { it.toFloat() }
      ?: emptyList()

  @TypeConverter
  @JvmStatic
  fun fromGradientType(type: GradientType?): String =
    type?.name ?: GradientType.LINEAR.name

  @TypeConverter
  @JvmStatic
  fun toGradientType(name: String?): GradientType =
    name
      ?.let { GradientType.valueOf(it) }
      ?: GradientType.LINEAR
}