package com.ssbmax.core.data.di

import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Plain `core:data` classes that Hilt previously auto-wired from their
 * `@Inject`-constructor without needing an explicit `@Provides`/`@Binds`
 * (mostly Firestore-backed repositories/data sources and Room-cache
 * managers with no separate domain interface). Koin has no implicit
 * constructor scanning, so each needs one explicit binding here instead.
 * All were `@Singleton`-scoped in Hilt (either directly or via the
 * `@Binds`/`@Provides` call site that consumed them).
 *
 * The submission-cluster classes (`FirestoreSubmissionRepository` and its
 * `CommonSubmissionRepository`/`SubmissionArchiveRepository`/`GTOSubmissionRepository`/
 * `PersonalTestSubmissionRepository`/`PsychTestSubmissionRepository` delegates, and their own
 * further delegates) were removed in the KMP-convergence plan's Phase 9e — `shared`'s
 * `GitLiveSubmissionRepository` is now the sole `SubmissionRepository` binding.
 */
val coreDataInjectablesModule = module {
    singleOf(::AnalyticsManager)
    singleOf(::DifficultyProgressionManager)
    singleOf(::FirebaseAuthService)
    singleOf(::FirebaseInitializer)
    singleOf(::FirestoreUserRepository)
}
