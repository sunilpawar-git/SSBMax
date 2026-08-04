package com.ssbmax.shared.di

import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.CrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the Swift-supplied Firebase Crashlytics/Analytics implementations
 * into the DI graph. Deliberately NOT part of [platformModule], unlike every
 * other iOS binding: these two cannot be constructed by Kotlin at all.
 *
 * They used to be `IosCrashReporter`/`IosAnalyticsTracker`, which cinterop'd
 * raw `FIRCrashlytics`/`FIRAnalytics` Objective-C APIs. Reaching those from
 * Kotlin required `pod("FirebaseCrashlytics")`/`pod("FirebaseAnalytics")` in
 * `shared/build.gradle.kts` — a *second* copy of the Firebase native
 * dependency graph alongside the one `iosApp.xcodeproj` already resolves via
 * SPM, and the only two pods that could not be declared `linkOnly` (raw
 * cinterop needs the Kotlin bindings that `linkOnly` suppresses).
 *
 * Inverting the dependency removes both pods: Swift already owns Firebase
 * (SPM), so Swift implements these two Kotlin interfaces and hands them in
 * at launch. Same shape as `MainViewController`'s `googleSignInLauncher`,
 * where Swift likewise owns the SPM dependency (`GoogleSignIn-iOS`) and
 * Kotlin only declares the interface.
 *
 * Scope of the win, stated honestly: this shrinks the CocoaPods surface from
 * five pods to three and removes Kotlin's only raw Firebase cinterop, but it
 * does NOT end the concurrent-`xcodebuild` races -- the three remaining
 * `linkOnly` pods still each get a `podBuild<Name>Ios*` task (verified by a
 * real test run; see the CORRECTION note in `shared/build.gradle.kts`). The
 * `.xcscheme` and `mustRunAfter` guards there, and the Run Script's
 * `indexbuild` guard, all stay until the `cocoapods {}` block goes away
 * entirely.
 *
 * See `iosApp/iosApp/FirebaseObservability.swift`.
 */
fun iosObservabilityModule(
    crashReporter: CrashReporter,
    analyticsTracker: AnalyticsTracker
): Module = module {
    single { crashReporter }
    single { analyticsTracker }
}
