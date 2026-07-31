package com.ssbmax.shared.di

import org.koin.test.check.checkModules
import org.koin.test.KoinTest
import kotlin.test.Test

/**
 * Phase 0d (KMP-convergence plan): pins that every constructor dependency in
 * `sharedModule` resolves against the iOS `platformModule` actual -- this,
 * not `commonTest`, because Koin's `androidContext()` extension used by the
 * Android actual needs a real/stubbed `Context` in the container before
 * resolution (see [PlatformModuleCheckTest] in `app/src/test`, the other
 * half of this check), which `commonTest` can't provide.
 *
 * UNVERIFIED as written: this session's Kotlin Native compiler couldn't
 * parse this dev machine's Xcode 26.6 SDK headers during `shared`'s Firebase
 * cinterop (a local Kotlin/Xcode version mismatch, unrelated to this test or
 * to the cocoapods integration it needs -- see `shared/build.gradle.kts`),
 * so this never actually ran here. [PlatformModuleCheckTest]'s equivalent
 * needed several rounds of real-instantiation fallout (Firebase throwing
 * without a configured FirebaseApp, ViewModels needing a test coroutine
 * dispatcher, a real Play Billing bug) before it went green -- expect this
 * one to need the same kind of iteration (likely `FirebaseApp.configure()`
 * or an equivalent stand-in, plus whatever iOS's `platformModule` actual
 * needs) once CI's pinned-Xcode-16 job actually runs it for the first time.
 */
class SharedModuleCheckTest : KoinTest {

    @Test
    fun `sharedModule resolves every dependency`() {
        checkModules {
            modules(sharedModule)
        }
    }
}
