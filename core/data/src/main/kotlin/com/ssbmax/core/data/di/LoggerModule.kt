package com.ssbmax.core.data.di

import com.ssbmax.core.data.util.AndroidDomainLogger
import com.ssbmax.shared.domain.util.DomainLogger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module providing the domain-layer logging implementation.
 *
 * Binds AndroidDomainLogger as the implementation of DomainLogger interface,
 * allowing domain layer use cases to log without Android dependencies.
 */
val loggerModule = module {
    singleOf(::AndroidDomainLogger) bind DomainLogger::class
}
