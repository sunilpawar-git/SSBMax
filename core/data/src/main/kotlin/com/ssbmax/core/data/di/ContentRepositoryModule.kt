package com.ssbmax.core.data.di

import com.ssbmax.core.data.repository.StudyContentRepositoryImpl
import com.ssbmax.core.domain.repository.StudyContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing content repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ContentRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindStudyContentRepository(
        impl: StudyContentRepositoryImpl
    ): StudyContentRepository
}

