import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// SSOT target of the KMP-convergence plan: `shared` is where business logic,
// data, and UI converge. `core:data` was fully dissolved into this module
// (Phase 9f); `app`'s UI/ViewModel/navigation layers dissolved into it earlier
// (Phase 5/6a) -- `app` itself persists by design as Android platform glue
// (MainActivity, notifications, WorkManager workers), not an ongoing
// dissolution target. See the plan's phase list for history.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler.kmp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.cocoapods)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // Apple Silicon dev machine (uname -m == arm64) -> simulator target is
    // iosSimulatorArm64. iosArm64 (device) is also declared since it's zero
    // extra cost to configure and keeps the target list "real" rather than
    // simulator-only, but only the simulator target was actually exercised
    // in this spike (no physical device available).
    val xcfName = "SharedKit"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    // Phase 0d/0b (KMP-convergence plan): GitLive's Firebase Kotlin/Native
    // artifacts are published as CocoaPods-shaped cinterops -- their klibs'
    // own manifests declare `linkerOpts=-framework FirebaseAuth` etc. and
    // `package=cocoapods.FirebaseAuth` (verified by unzipping and inspecting
    // them directly). `iosApp`'s own Firebase integration uses Swift Package
    // Manager, which compiles FirebaseCore/Auth/Storage straight into the app
    // binary as object code -- it never produces the standalone .framework
    // bundles Kotlin/Native needs to link against, which is exactly why
    // `:shared:iosSimulatorArm64Test` couldn't link before this block existed
    // (confirmed: it failed identically on the pre-existing suite, before any
    // of this phase's tests were added). This block is scoped to `:shared`'s
    // own Kotlin/Native compilation only -- it resolves these pods into a
    // synthetic Xcode project purely to build real frameworks for the linker;
    // `iosApp` keeps using SPM and is untouched by it.
    cocoapods {
        summary = "SSBMax shared KMP module"
        homepage = "https://github.com/ssbmax/ssbmax"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        // No `podfile = ...` here on purpose: leaving it unset makes the
        // plugin generate its own synthetic Podfile/Xcode project under
        // build/cocoapods/synthetic/ to resolve these pods, rather than
        // requiring (or writing into) a Podfile in `iosApp`.

        // linkOnly = true on the Firebase pods this module only consumes via
        // GitLive's precompiled Kotlin bindings (no raw cinterop of our own):
        // `iosApp` links its OWN copy of Firebase via SPM (see iosApp.xcodeproj),
        // separate from this cocoapods block, which otherwise exists purely to
        // give Kotlin/Native's cinterop something to compile GitLive's bindings
        // against. Without linkOnly, these pods' real .framework binaries get
        // compiled into SharedKit.framework (static) AND iosApp separately links
        // its own SPM Firebase copy into the same app binary -- two copies of
        // Firebase's ObjC classes/symbols in one process, a known duplicate-symbol
        // dyld crash risk GitLive's own docs warn about for exactly this
        // CocoaPods+SPM split. linkOnly=true makes this block satisfy the
        // Kotlin/Native linker at `shared`'s build time without embedding a
        // second copy -- iosApp's SPM-linked Firebase remains the single real
        // copy at runtime.
        pod("FirebaseAuth") { linkOnly = true }
        pod("FirebaseFirestore") { linkOnly = true }
        pod("FirebaseStorage") { linkOnly = true }
        // Phase 7a (KMP-convergence plan): CrashReporter/AnalyticsTracker
        // seams -- see IosCrashReporter.kt/IosAnalyticsTracker.kt. Linking
        // these into `SharedKit.framework` here is necessary but not
        // sufficient: `iosApp.xcodeproj`'s own SPM product list must also
        // add FirebaseCrashlytics/FirebaseAnalytics for the shipped app to
        // actually carry them (a manual Xcode step, not reachable from
        // Gradle -- see the Phase 7a summary for why this is named rather
        // than done here).
        //
        // NOT linkOnly, unlike the three above: IosCrashReporter.kt/
        // IosAnalyticsTracker.kt cinterop directly against these pods' raw
        // Obj-C APIs (FIRCrashlytics, FIRAnalytics) rather than going through
        // GitLive -- linkOnly suppresses Kotlin binding generation entirely,
        // which broke those two files ("Unresolved reference 'FIRCrashlytics'"
        // etc). The double-link risk these two theoretically carry is
        // unconfirmed (unlike Auth/Firestore/Storage, no GitLive Kotlin API
        // consumes them, and iosApp's own SPM product list doesn't currently
        // declare FirebaseCrashlytics/FirebaseAnalytics per the comment above)
        // -- revisit if/when Crashlytics/Analytics are wired into iosApp's SPM
        // list.
        pod("FirebaseCrashlytics")
        pod("FirebaseAnalytics")
    }

    applyDefaultHierarchyTemplate()

    // Kotlin 2.2.20 (bumped for Xcode 26 SDK support, see gradle/libs.versions.toml)
    // ships kotlin.time.Clock/Instant as @ExperimentalTime -- kotlinx-datetime 0.7.1's
    // kotlinx.datetime.Clock/Instant are now deprecated typealiases for those stdlib
    // types, so every pre-existing `Clock.System.now()` / `Instant.fromEpochMilliseconds()`
    // call site across shared/commonMain needs this opt-in (some surfaced as "needs
    // opt-in", others cascaded into a misleading "unresolved reference" -- same cause,
    // fixed for real by importing kotlin.time.Clock/Instant directly instead of the
    // deprecated kotlinx.datetime typealiases, which weren't propagating the opt-in
    // correctly across Kotlin/Native metadata compilation).
    // ExperimentalForeignApi: IosCrashReporter/IosAnalyticsTracker's raw Objective-C
    // interop (NSError, FIRCrashlytics) started needing this opt-in too under 2.2.20's
    // stricter checking -- it wasn't annotated before and evidently wasn't enforced.
    // Blanket module opt-in instead of annotating individual files.
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            // Phase 3c (#12): commonMain `BackHandler` -- see the version
            // catalog entry's own comment for why this needs a direct
            // declaration.
            implementation(libs.compose.ui.backhandler)
            // Phase 5: Login/RoleSelection need Icons.AutoMirrored.Filled.Login /
            // Icons.Default.{School,Groups}, which aren't in the small default
            // icon set bundled with compose.material3 -- this is the JetBrains
            // Compose Multiplatform-published extended icon pack (not the
            // Android-only androidx-compose-material-icons-extended catalog
            // alias `app` uses, which has no common variant).
            implementation(compose.materialIconsExtended)

            // Phase 5 (UI): navigation + image loading, both real KMP artifacts
            // (versions verified against maven-central metadata before adding).
            implementation(libs.navigation.compose.kmp)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor3)
            implementation(libs.androidx.lifecycle.runtime.compose.kmp)
            implementation(libs.androidx.lifecycle.viewmodel.compose.kmp)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.common)
            // Not re-exported as `api` by firebase-common/-firestore (implementation-only
            // upstream), so it must be depended on directly to reach the public
            // FirebaseDecoder/FirebaseEncoder classes used by FirestoreRawMapSerializer for the
            // raw Map<String, Any> Firestore decode/encode path. See that file's class doc.
            implementation(libs.gitlive.firebase.common.internal)
            implementation(libs.gitlive.firebase.storage)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
            // Turbine is pure Kotlin/coroutines (JVM+JS+Native) -- unlike MockK it has
            // a real KMP artifact, so Phase 1's ViewModel characterization tests run
            // it here to cover iOS too, not just androidUnitTest below.
            implementation(libs.turbine)
            // Compose Multiplatform's own commonTest UI-test artifact (real KMP target,
            // unlike androidx.compose.ui:ui-test-junit4 which is Android-only) -- declared
            // here so it's available to every target's runComposeUiTest, including iOS's
            // Native test harness later. Phase 2's own SSBMaxThemeUiTest currently lives in
            // androidUnitTest only (needs Robolectric, see that source set below); full
            // cross-platform UI-test porting is Phase 6a's larger undertaking.
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        // Phase 1 domain move: the 6 MockK-dependent unit tests from core:domain's
        // JVM-only test source set land here (androidUnitTest), not commonTest --
        // MockK has no KMP/native equivalent, so they stay JUnit4 + MockK on the
        // Android target only.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
                // Phase 2: JVM SQLDelight driver to actually exercise CachedOirResult
                // reads/writes against a real in-memory SQLite DB in unit tests --
                // closes the Phase 0 exit report's "SQLDelight unexercised at
                // runtime" gap without needing an emulator/simulator.
                implementation(libs.sqldelight.sqlite.driver)
                // Phase 2: `androidx.compose.ui.test.runComposeUiTest`'s Android host-side
                // implementation reads `android.os.Build.FINGERPRINT`, which is null without
                // Robolectric's shadow classes -- same dependency `app` already uses for its
                // own Robolectric-backed unit tests. `@RunWith(RobolectricTestRunner::class)`
                // is JUnit4-only (no Kotlin/Native equivalent), so the one CMP UI test this
                // phase adds (SSBMaxThemeUiTest) lives in this androidUnitTest source set,
                // not commonTest -- porting the rest of the suite to run on iOS too is
                // Phase 6a's larger undertaking.
                implementation(libs.robolectric)
            }
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.billing.ktx)
            // Phase 5: legacy GoogleSignInClient, matching the pre-KMP
            // FirebaseAuthService flow -- see AndroidGoogleSignInLauncher.
            implementation(libs.play.services.auth)
            // GitLive's Android artifacts declare com.google.firebase:* deps
            // without version pins (expects the consumer to apply Firebase's
            // BOM) -- confirmed via a real build failure ("Could not find
            // com.google.firebase:firebase-auth-ktx:") before this was added.
            implementation(project.dependencies.platform(libs.firebase.bom))
            // Phase 7a: AndroidCrashReporter/AndroidAnalyticsTracker actuals.
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.analytics)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    lintChecks(project(":lint"))
}

android {
    namespace = "com.ssbmax.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    // Robolectric-only launcher Activity for SSBMaxThemeUiTest's
    // runComposeUiTest -- see shared/src/androidUnitTest/AndroidManifest.xml's
    // own doc comment. Merged only into the JVM unit test manifest.
    // includeAndroidResources is required for AGP to actually merge/honor a
    // per-test-sourceSet manifest for a library module's unit tests --
    // confirmed empirically: no processDebugUnitTestManifest task exists,
    // and the override is silently ignored, without it.
    testOptions.unitTests.isIncludeAndroidResources = true
    sourceSets.getByName("test") {
        manifest.srcFile("src/androidUnitTest/AndroidManifest.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        // NonNullableMutableLiveDataDetector (bundled in AGP's androidx-lifecycle
        // lint checks) crashes with IncompatibleClassChangeError on this module's
        // Compose Multiplatform-generated ActualResourceCollectors.kt -- a lint
        // tooling/UAST version mismatch, not a real violation. Workaround is the
        // one lint's own crash message suggests.
        disable += "NullSafeMutableLiveData"
    }
}

sqldelight {
    databases {
        create("SharedDatabase") {
            packageName.set("com.ssbmax.shared.db")
        }
    }
}

// Layers config/detekt/detekt-shared.yml (the "ssbmax" custom rule set
// activation) on top of the repo-wide config -- see that file's own doc for
// why it can't just live in config/detekt/detekt.yml directly.
detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-shared.yml"))
}
