package com.ssbmax.shared.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.ssbmax.shared.platform.auth.GoogleSignInLauncher
import com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher
import com.ssbmax.shared.platform.ensureKoinStarted
import com.ssbmax.shared.platform.permissions.NotificationPermissionController
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController
import org.koin.compose.koinInject

/**
 * iOS entry point. Koin is started via [ensureKoinStarted] -- as of Phase 6,
 * the real bootstrap happens earlier, from `AppDelegate.swift`'s
 * `application(_:didFinishLaunchingWithOptions:)` (needed before
 * `BGTaskScheduler` registration/APNs token hand-off, both Koin-dependent).
 * The call here is a guarded no-op in that case; kept as a defensive
 * fallback for any future entry point that renders this Composable without
 * going through `AppDelegate` first.
 *
 * Phase 5: renders the real nav graph + chrome (drawer/bottom nav) via
 * [SSBMaxRoot] instead of the Phase 0 single-screen demo it replaced — the
 * first real multi-screen, chrome-wrapped flow exercised on iOS.
 *
 * Phase 2: [SSBMaxRoot] also closes iOS's total theming gap — this
 * Composable previously wrapped everything in a bare `MaterialTheme { }`
 * with no branded colors/typography and no reaction to the user's theme
 * preference at all. [SSBMaxRoot] now owns theme collection/application, so
 * this function's only remaining direct responsibilities are Koin bootstrap
 * and the two platform CompositionLocals below.
 *
 * [googleSignInLauncher] is provided directly here rather than via Koin,
 * matching the Android side's `MainActivity`-constructed,
 * CompositionLocal-provided pattern; iOS has no equivalent
 * `ActivityResultLauncher` lifecycle constraint. It defaults to
 * [IosGoogleSignInLauncher] (a STUB — see its class doc) so this Composable
 * still compiles/renders standalone, but `ContentView.swift` always passes
 * a real `GIDSignIn`-backed implementation (see
 * `RealIosGoogleSignInLauncher.swift`) since Swift, not Kotlin, owns the
 * `GoogleSignIn-iOS` SPM dependency and the presenting `UIViewController`
 * `GIDSignIn.sharedInstance.signIn(withPresenting:)` needs.
 *
 * [NotificationPermissionController], unlike the Google Sign-In launcher, IS
 * a plain Koin single on iOS (`PlatformModule.ios.kt` -- `UNUserNotificationCenter`
 * has no `ActivityResultLauncher`-style registration constraint), so it's
 * resolved via [koinInject] here rather than threaded through as a
 * parameter. Missing this provider entirely (rather than the Google
 * Sign-In stub's intentional throw) was a real gap: `StudentHomeScreen`/
 * `StartInterviewScreen` read [LocalNotificationPermissionController]
 * unconditionally, so any screen touching notification permissions crashed
 * on iOS with "No NotificationPermissionController provided" the moment it
 * composed -- discovered via a real launch crash reaching `StudentHomeScreen`.
 */
fun MainViewController(
    googleSignInLauncher: GoogleSignInLauncher = IosGoogleSignInLauncher()
) = ComposeUIViewController {
    ensureKoinStarted()
    val notificationPermissionController = koinInject<NotificationPermissionController>()
    CompositionLocalProvider(
        LocalGoogleSignInLauncher provides googleSignInLauncher,
        LocalNotificationPermissionController provides notificationPermissionController
    ) {
        SSBMaxRoot()
    }
}
