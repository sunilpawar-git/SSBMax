package com.ssbmax.shared.platform.auth

import com.ssbmax.shared.domain.model.GoogleSignInData

/**
 * iOS actual — fallback STUB, only reached if [MainViewController] is ever
 * rendered without a `googleSignInLauncher` argument. In the real app,
 * `ContentView.swift` always supplies `RealIosGoogleSignInLauncher` (backed
 * by the `GoogleSignIn-iOS` SPM package + `GIDSignIn`), added once that
 * native dependency was wired into `iosApp.xcodeproj` (Phase 6 iOS shell).
 *
 * Kept as a real implementation of [GoogleSignInLauncher] (rather than
 * deleted) so this Composable still compiles/previews standalone. Fails
 * loudly rather than silently returning a fake success or a permanent
 * [GoogleSignInData.Cancelled] — per this plan's "fail loud" rule (CLAUDE.md
 * rule 12) — in the unexpected case this fallback is ever actually reached.
 */
class IosGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun signIn(): GoogleSignInData {
        throw NotImplementedError(
            "iOS Google Sign-In is not implemented yet: it requires Google's " +
                "native GoogleSignIn-iOS SDK (SPM/CocoaPods), not yet a project " +
                "dependency. GitLive's firebase-auth alone cannot produce a Google " +
                "ID token on iOS. Tracked as Phase 6 (iOS shell) scope."
        )
    }
}
