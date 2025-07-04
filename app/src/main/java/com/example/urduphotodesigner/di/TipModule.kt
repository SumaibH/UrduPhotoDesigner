package com.example.urduphotodesigner.di

import com.example.urduphotodesigner.data.repository.TipManagerImpl
import com.example.urduphotodesigner.domain.repo.TipManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TipModule {
  @Binds
  @Singleton
  abstract fun bindTipManager(
    impl: TipManagerImpl
  ): TipManager
}
