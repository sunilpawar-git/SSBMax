package com.ssbmax.core.data.di

import org.koin.dsl.module

/**
 * Koin bindings for repository implementations — one binding per interface,
 * mirroring the former Hilt `RepositoryModule` (`@Binds @Singleton`) in
 * `DataModule.kt`.
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
 *
 * As of Phase 9e (KMP-convergence plan), every repository this module used to
 * shadow-bind (19 at plan-authoring time) has been deleted — `SubmissionRepository`/
 * `TestSubmissionRepository`/`TestSessionRepository` (write-heavy submissions/session state, the
 * highest-risk/last sub-phase) were the final three. `shared`'s `GitLive*` equivalents are now the
 * sole binding for all of them. This module is now empty; kept (not deleted) as the named,
 * documented landing spot in case a future `core:data` repository binding is ever needed again —
 * Phase 9f (module retirement) is the one that removes `core:data` itself.
 */
val repositoryModule = module {}
