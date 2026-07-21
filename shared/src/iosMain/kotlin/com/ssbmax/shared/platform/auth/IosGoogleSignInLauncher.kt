package com.ssbmax.shared.platform.auth

import com.ssbmax.shared.domain.model.GoogleSignInData

/**
 * iOS actual — STUB. See [GoogleSignInLauncher]'s class doc for the full
 * rationale: GitLive's `firebase-auth` KMP SDK only consumes a Google ID
 * token once obtained, it does not provide the native sign-in picker itself.
 * Getting that token on iOS requires Google's `GoogleSignIn-iOS` SDK, which
 * is not yet an SPM/CocoaPods dependency of this repo.
 *
 * Fails loudly rather than silently returning a fake success or a permanent
 * [GoogleSignInData.Cancelled] — per this plan's "fail loud" rule (CLAUDE.md
 * rule 12), a caller must see this break, not silently no-op. Real
 * implementation is Phase 6 (iOS shell) scope, once the native dependency is
 * added.
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
