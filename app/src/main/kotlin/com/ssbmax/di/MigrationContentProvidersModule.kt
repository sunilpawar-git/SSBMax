package com.ssbmax.di

import com.ssbmax.core.data.repository.MigrationContentProviders
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing MigrationContentProviders implementation
 *
 * Architecture Note:
 * This module lives in the app layer (not data layer) because the implementation
 * depends on UI layer providers. This is a temporary arrangement during the migration
 * process. Once UI providers are refactored to repositories, this module can be removed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MigrationContentProvidersModule {

    /**
     * Binds MigrationContentProviders interface to its bridge implementation
     *
     * This allows the data layer (MigrationRepositoryImpl) to receive the
     * UI provider adapter without directly depending on UI layer classes.
     */
    @Binds
    @Singleton
    abstract fun bindMigrationContentProviders(
        impl: MigrationContentProvidersImpl
    ): MigrationContentProviders
}
