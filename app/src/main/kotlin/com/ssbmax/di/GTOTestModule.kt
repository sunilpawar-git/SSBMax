package com.ssbmax.di

import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.ui.tests.gto.common.GTOSequentialAccessManager
import com.ssbmax.ui.tests.gto.common.GTOTestEligibilityChecker
import com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
import com.ssbmax.ui.tests.gto.common.GTOWhiteNoisePlayer
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for GTO test helper classes
 */
@Module
@InstallIn(SingletonComponent::class)
object GTOTestModule {

    @Provides
    @Singleton
    fun provideGTOTestEligibilityChecker(
        observeCurrentUser: ObserveCurrentUserUseCase,
        userProfileRepository: UserProfileRepository,
        subscriptionManager: SubscriptionManager,
        sequentialAccessManager: GTOSequentialAccessManager,
        securityLogger: SecurityEventLogger
    ): GTOTestEligibilityChecker {
        return GTOTestEligibilityChecker(
            observeCurrentUser = observeCurrentUser,
            userProfileRepository = userProfileRepository,
            subscriptionManager = subscriptionManager,
            sequentialAccessManager = sequentialAccessManager,
            securityLogger = securityLogger
        )
    }

    @Provides
    @Singleton
    fun provideGTOTestSubmissionHelper(
        gtoRepository: GTORepository,
        workManager: WorkManager
    ): GTOTestSubmissionHelper {
        return GTOTestSubmissionHelper(
            gtoRepository = gtoRepository,
            workManager = workManager
        )
    }
}

/**
 * Hilt Entry Point for accessing GTOWhiteNoisePlayer in Compose screens
 * 
 * This allows non-ViewModel dependencies to be injected in Compose using hiltEntryPoint()
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GTOWhiteNoisePlayerEntryPoint {
    fun whiteNoisePlayer(): GTOWhiteNoisePlayer
}
