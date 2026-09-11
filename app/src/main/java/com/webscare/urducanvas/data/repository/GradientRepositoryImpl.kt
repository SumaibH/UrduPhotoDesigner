package com.webscare.urducanvas.data.repository

import android.util.Log
import com.webscare.urducanvas.common.canvas.model.GradientItem
import com.webscare.urducanvas.data.mapper.toDomain
import com.webscare.urducanvas.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.collections.map
/**
 * Log tag for this file. Was android.content.ContentValues.TAG, an accidental import that
 * filed every one of these messages under "ContentValues".
 */
private const val TAG = "GradientRepositoryImpl"

class GradientRepositoryImpl(
  private val dao: com.webscare.urducanvas.data.local.GradientDao
) : com.webscare.urducanvas.domain.repo.GradientRepo {

  override fun getAllGradients(): Flow<List<GradientItem>> =
    dao.getAll().map { list -> list.map { it.toDomain() } }

  /**
   * Puts any default that is not already in the table into it.
   *
   * This used to seed only when the table was empty, which meant a gradient added to
   * [com.webscare.urducanvas.common.utils.GradientPresets] after a user's first launch
   * never reached them — the catalogue was frozen at whatever shipped the day they
   * installed. Matching on what a gradient *is* rather than on a row count fixes that,
   * leaves gradients the user made alone, and does nothing at all on the runs where
   * there is nothing new.
   */
  override suspend fun seedDefaultGradients(defaults: List<com.webscare.urducanvas.common.canvas.model.GradientItem>) {
    val existing = dao.getAll().first().map { it.toDomain().signature() }.toHashSet()
    val missing = defaults.filterNot { it.signature() in existing }
    if (missing.isEmpty()) return
    Log.d(TAG, "seedDefaultGradients: adding ${missing.size} of ${defaults.size}")
    dao.insertAll(missing.map { it.toEntity() })
  }

  /** Everything that makes two gradients look the same. Ids are deliberately not in it. */
  private fun com.webscare.urducanvas.common.canvas.model.GradientItem.signature(): String =
    listOf(
      colors.joinToString(","), positions.joinToString(","),
      angle, scale, type, radialRadiusFactor, sweepStartAngle, centerX, centerY
    ).joinToString("|")

  override suspend fun insertNewGradient(gradient: com.webscare.urducanvas.common.canvas.model.GradientItem) {
    dao.insert(gradient.toEntity())
  }

  override suspend fun deleteGradientById(id: Long) {
    dao.deleteById(id)
  }

  override suspend fun updateGradient(gradient: com.webscare.urducanvas.common.canvas.model.GradientItem) {
    dao.updateGradient(gradient.toEntity())
  }
}