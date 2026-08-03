package com.ssbmax.core.data.di

import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.SubscriptionRepositoryImpl
import com.ssbmax.core.data.repository.TestSessionManagerImpl
import com.ssbmax.core.data.repository.TestSubmissionRepositoryImpl
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestSubmissionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin bindings for repository implementations — one binding per interface,
 * mirroring the former Hilt `RepositoryModule` (`@Binds @Singleton`) in
 * `DataModule.kt`. [SubscriptionManager] binds to both its concrete type
 * (still constructor-injected directly by [com.ssbmax.di.gtoTestModule] and
 * [com.ssbmax.di.debugModule]) and [TestUsageRecorder].
 *
 * `AuthRepository` is deliberately NOT bound here (Phase 5 fix — was
 * previously `singleOf(::AuthRepositoryImpl) bind AuthRepository::class`,
 * which silently double-bound the same interface `:shared`'s `sharedModule`
 * also binds via `GitLiveAuthRepository`; Koin's default `allowOverride`
 * let whichever module loaded later in `appModules`' list win with no
 * compile-time signal). The two impls are NOT interchangeable:
 * `AuthRepositoryImpl.handleGoogleSignInResult` expects
 * `GoogleSignInData.ResultData.platformData` to be an Android `Intent`
 * (legacy `GoogleSignInClient` flow), while `GitLiveAuthRepository` expects
 * a `Pair<String, String?>` (idToken, accessToken) — the convention the new
 * `GoogleSignInLauncher` shim (`shared/commonMain/platform/auth`) produces.
 * `GitLiveAuthRepository` is now the sole `AuthRepository` binding, matching
 * the new commonMain auth vertical (`LoginScreen`/`AuthViewModel` in
 * `:shared`) that this phase wires up. `AuthRepositoryImpl`/
 * `FirebaseAuthService` (this module) are left in place, unbound and
 * untouched — a separate class-level cleanup, not forced here (see this
 * phase's exit report).
 * Every repository still bound here (5, as of Phase 9c) has the same kind of
 * shadow-bound `GitLive*` equivalent in `sharedModule`, ready for the same
 * "delete this binding + impl" treatment the KMP-convergence plan's Phase 9
 * already applied to the other 13 — `SubmissionRepository`/`TestUsageRecorder`
 * (9d, revenue-sensitive) and `TestSubmissionRepository`/`TestSessionRepository`
 * (9e, write-heavy submissions/session state) are what remain.
 */
val repositoryModule = module {
    singleOf(::FirestoreSubmissionRepository) bind SubmissionRepository::class
    singleOf(::TestSubmissionRepositoryImpl) bind TestSubmissionRepository::class
    singleOf(::SubscriptionRepositoryImpl) bind SubscriptionRepository::class
    singleOf(::SubscriptionManager) bind TestUsageRecorder::class
    singleOf(::TestSessionManagerImpl) bind TestSessionRepository::class
}
