package com.ssbmax.di

import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module bridging :shared (KMP) subscription use cases into the Android Hilt graph.
 *
 * See [AuthUseCaseModule] for why these bindings are explicit rather than
 * `@Inject`-constructor based.
 */
@Module
@InstallIn(SingletonComponent::class)
object SubscriptionUseCaseModule {

    @Provides
    fun provideGetMonthlyUsageUseCase(
        subscriptionRepository: SubscriptionRepository
    ): GetMonthlyUsageUseCase = GetMonthlyUsageUseCase(subscriptionRepository)

    @Provides
    fun provideGetSubscriptionTierUseCase(
        subscriptionRepository: SubscriptionRepository
    ): GetSubscriptionTierUseCase = GetSubscriptionTierUseCase(subscriptionRepository)
}
