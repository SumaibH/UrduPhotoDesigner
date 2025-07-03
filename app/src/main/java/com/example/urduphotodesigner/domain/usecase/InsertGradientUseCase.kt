package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.common.canvas.model.GradientItem
import com.example.urduphotodesigner.domain.repo.GradientRepository

class InsertGradientUseCase(private val repo: GradientRepository) {
    suspend operator fun invoke(gradient: GradientItem) =
        repo.insertNewGradient(gradient)
}