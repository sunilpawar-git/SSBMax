package com.ssbmax.core.data.di

import com.ssbmax.core.data.repository.MigrationRepositoryImpl
import com.ssbmax.core.domain.repository.MigrationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for migration-related dependency injection
 *
 * Provides bindings for migration repository implementations.
 * This module is installed in SingletonComponent to ensure repository instances
 * are shared application-wide.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MigrationModule {

    /**
     * Binds MigrationRepository interface to its implementation
     *
     * @param impl The concrete implementation injected by Hilt
     * @return The repository interface for domain layer use
     */
    @Binds
    @Singleton
    abstract fun bindMigrationRepository(
        impl: MigrationRepositoryImpl
    ): MigrationRepository
}
