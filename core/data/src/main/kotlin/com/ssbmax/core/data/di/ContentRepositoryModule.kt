package com.ssbmax.core.data.di

import com.ssbmax.core.data.repository.StudyContentRepositoryImpl
import com.ssbmax.shared.domain.repository.StudyContentRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for providing content repository implementations.
 */
val contentRepositoryModule = module {
    singleOf(::StudyContentRepositoryImpl) bind StudyContentRepository::class
}
