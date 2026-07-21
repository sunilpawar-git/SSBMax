package com.ssbmax.shared.platform.auth

import com.ssbmax.shared.domain.model.GoogleSignInData

/**
 * Launches the platform's Google Sign-In UI and returns the resulting
 * credential wrapped in [GoogleSignInData], ready to hand straight to
 * `SignInWithGoogleUseCase`.
 *
 * This is the real native-picker trigger that
 * `GitLiveAuthRepository.getGoogleSignInIntent()`'s Phase 0/2 placeholder
 * (returns `GoogleSignInData.LaunchData(Unit)`, never wired to an actual
 * platform UI — see that class's doc comment) deferred; Phase 5 wires it.
 *
 * Deliberately NOT constructor-injected into [com.ssbmax.shared.presentation.auth.AuthViewModel]
 * via Koin: the Android actual wraps an `ActivityResultLauncher` that must
 * be registered by the hosting `ComponentActivity` before it reaches
 * `STARTED` (same platform constraint documented on
 * `AndroidNotificationPermissionController` — Koin's DI graph can't
 * guarantee that ordering). Instead this is supplied to `LoginScreen` via
 * `LocalGoogleSignInLauncher` (a `CompositionLocal`, same pattern as
 * `LocalNotificationPermissionController`) and the resulting
 * [GoogleSignInData] is handed to the ViewModel as a plain method
 * parameter, not resolved through the constructor.
 *
 * Implementations:
 * - Android ([com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher]):
 *   legacy `GoogleSignInClient` + `ActivityResultLauncher<Intent>`, matching
 *   the pre-KMP `FirebaseAuthService`/`AuthRepositoryImpl` flow byte-for-byte
 *   (webClientId read from `google-services.json`'s generated string
 *   resource, idToken + null accessToken). Produces a
 *   `GoogleSignInData.ResultData` wrapping `Pair<String, String?>`
 *   (idToken, accessToken) — the convention `GitLiveAuthRepository`
 *   expects, NOT an Android `Intent` (that was `AuthRepositoryImpl`'s old,
 *   now-unbound convention — see `RepositoryModule`'s doc comment for why
 *   the two are incompatible and only one can be wired at a time).
 * - iOS ([com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher]): STUB.
 *   GitLive's `firebase-auth` KMP SDK only *consumes* a Google ID token via
 *   `GoogleAuthProvider.credential(...)` once you already have one — it does
 *   not itself provide a Google Sign-In picker UI. Obtaining that token
 *   natively requires Google's own `GoogleSignIn-iOS` SDK (SPM/CocoaPods),
 *   which is not yet a dependency of this repo. Rather than fake a working
 *   flow, the iOS actual fails loudly (throws) until that native dependency
 *   is added — tracked as Phase 6 (iOS shell) follow-up, not silently
 *   stubbed to a fake success per this plan's "fail loud" rule.
 */
interface GoogleSignInLauncher {
    /**
     * Launches the Google Sign-In flow and suspends until the user completes
     * or cancels it.
     *
     * @return [GoogleSignInData.ResultData] wrapping a
     *   `Pair<String, String?>` (idToken, accessToken?) on success,
     *   [GoogleSignInData.Cancelled] if the user backed out, or
     *   [GoogleSignInData.Error] on failure.
     */
    suspend fun signIn(): GoogleSignInData
}
