package com.ssbmax.di

import com.ssbmax.shared.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module bridging :shared (KMP) subscription use cases into the app's Koin graph.
 *
 * See [authUseCaseModule] for why these bindings are explicit rather than
 * relying on annotation processing.
 */
val subscriptionUseCaseModule = module {
    factoryOf(::GetMonthlyUsageUseCase)
    factoryOf(::GetSubscriptionTierUseCase)
}
