package com.ssbmax.di

import androidx.work.WorkManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.ui.tests.gto.common.GTOSequentialAccessManager
import com.ssbmax.ui.tests.gto.common.GTOTestEligibilityChecker
import com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
import org.koin.dsl.module

/**
 * Koin module for GTO test helper classes.
 *
 * `GTOWhiteNoisePlayer` no longer needs the Hilt-EntryPoint-style
 * indirection ([com.ssbmax.ui.tests.gto.common.GTOWhiteNoisePlayer] used to
 * be reached via `GTOWhiteNoisePlayerEntryPoint`/`EntryPointAccessors` in
 * Compose screens that aren't ViewModels) — Koin has no
 * ViewModel-vs-plain-Composable distinction, so `GDTestScreen`/
 * `LecturetteTestScreen` now resolve it directly via `koinInject()`. See
 * [appInjectablesModule] for its binding.
 */
val gtoTestModule = module {
    single {
        GTOTestEligibilityChecker(
            observeCurrentUser = get<ObserveCurrentUserUseCase>(),
            userProfileRepository = get<UserProfileRepository>(),
            subscriptionManager = get<SubscriptionManager>(),
            sequentialAccessManager = get<GTOSequentialAccessManager>(),
            securityLogger = get<SecurityEventLogger>()
        )
    }
    single {
        GTOTestSubmissionHelper(
            gtoRepository = get<GTORepository>(),
            submissionRepository = get<SubmissionRepository>(),
            workManager = get<WorkManager>()
        )
    }
}
