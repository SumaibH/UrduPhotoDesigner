package com.example.urduphotodesigner.data.mapper

import com.example.urduphotodesigner.common.canvas.model.GradientItem
import com.example.urduphotodesigner.data.model.GradientEntity

fun GradientEntity.toDomain() = GradientItem(
    id = id,
    colors = colors,
    positions = positions,
    angle = angle,
    scale = scale,
    type = type,
    radialRadiusFactor = radialRadiusFactor,
    sweepStartAngle = sweepStartAngle,
    centerX = centerX,
    centerY = centerY
)

fun GradientItem.toEntity() = GradientEntity(
    id = id,
    colors = colors,
    positions = positions,
    angle = angle,
    scale = scale,
    type = type,
    radialRadiusFactor = radialRadiusFactor,
    sweepStartAngle = sweepStartAngle,
    centerX = centerX,
    centerY = centerY
)