package com.ssbmax.di

import com.ssbmax.time.Clock
import com.ssbmax.time.SystemClock
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val clockModule = module {
    singleOf(::SystemClock) bind Clock::class
}
