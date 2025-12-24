package com.ssbmax.core.data.di

import com.ssbmax.core.data.util.AndroidDomainLogger
import com.ssbmax.core.domain.util.DomainLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing domain layer logging implementation
 * 
 * Binds AndroidDomainLogger as the implementation of DomainLogger interface,
 * allowing domain layer use cases to log without Android dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {
    
    @Binds
    @Singleton
    abstract fun bindDomainLogger(
        androidLogger: AndroidDomainLogger
    ): DomainLogger
}

