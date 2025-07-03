package com.example.urduphotodesigner.domain.usecase

import com.example.urduphotodesigner.domain.repo.GradientRepository

class DeleteGradientUseCase(private val repo: GradientRepository) {
  suspend operator fun invoke(id: Long) =
    repo.deleteGradientById(id)
}