import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Move 2 of the iOS CocoaPods->SPM convergence: the Firebase-backed
// implementations of `:shared`'s repository interfaces live here, above
// `:shared` rather than inside it.
//
// The reason is a linker fact, verified by a --dry-run of the task graph:
// `linkDebugFrameworkIosSimulatorArm64` depends on ZERO CocoaPods tasks,
// while `linkDebugTestIosSimulatorArm64` pulls all six. CocoaPods existed in
// this repo for exactly one job -- linking Kotlin/Native TEST executables,
// because GitLive's klibs carry `linkerOpts = -framework FirebaseAuth` and a
// fully-linked test binary must resolve them. The app never needed it:
// `SharedKit` is a static framework, so Xcode resolves Firebase from SPM at
// final app link.
//
// Moving GitLive out of `:shared` therefore removes Firebase from `:shared`'s
// klib graph, its test binaries link with no pods, and the entire CocoaPods
// integration can be deleted rather than merely relocated.
//
// LOAD-BEARING CONSTRAINT: this module must NOT gain an iOS test source set.
// Any `iosTest` here drags the GitLive klibs and their `-framework Firebase*`
// linker opts back into a Kotlin/Native test executable, and CocoaPods comes
// straight back with them. Tests for this module's code run on JVM/Android
// only -- the same constraint that already keeps the MockK-based tests in
// `androidUnitTest` (MockK has no KMP/Native artifact).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // `SharedKit` is produced HERE, not in `:shared`. This module sits at the
    // top of the Kotlin dependency graph, so only a framework built here
    // carries both `:shared`'s UI/ViewModels and the Firebase-backed
    // repository implementations the Koin graph needs. `export(project(":shared"))`
    // makes `:shared`'s public API visible to Swift (MainViewController,
    // SSBMaxRoot, the domain types) -- it works because this module declares
    // `:shared` as `api`, which `export` requires.
    //
    // No `cocoapods {}` block, deliberately: a --dry-run of the task graph
    // shows framework linking depends on ZERO pod tasks (`SharedKit` is
    // static, so Xcode resolves Firebase from SPM at final app link). Verified
    // end-to-end -- `:shared:iosSimulatorArm64Test` now links with no pods at
    // all, which is the entire point of Move 2.
    val xcfName = "SharedKit"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            isStatic = true
            export(project(":shared"))
        }
    }

    applyDefaultHierarchyTemplate()

    // Same opt-ins as `:shared` -- see that module's build file for the
    // Kotlin 2.2.20 / kotlinx-datetime Clock typealias rationale.
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: this module's whole purpose is to
            // supply implementations of `:shared`'s interfaces to the app
            // binaries, so consumers depending on this module must also see
            // `:shared`'s domain types, ViewModels and UI.
            api(project(":shared"))

            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.common)
            // See FirestoreRawMapSerializer's class doc: not re-exported as
            // `api` upstream, so it needs a direct declaration to reach
            // FirebaseDecoder/FirebaseEncoder.
            implementation(libs.gitlive.firebase.common.internal)
            implementation(libs.gitlive.firebase.storage)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)

            // GitLiveAnalyticsRepository / GitLiveNotificationCacheManager
            // read the SQLDelight-generated database `:shared` declares.
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }

        // NOTE: deliberately no `iosTest` source set -- see the class doc above.
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                // JVM SQLDelight driver: the cache-manager tests moved here from
                // `:shared` exercise reads/writes against a real in-memory SQLite
                // DB rather than mocking the queries, so they need a driver that
                // runs on the JVM without an emulator.
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
        }

        // The iOS entry points Swift calls into (AppBootstrap's
        // ensureKoinStarted/sharedKoin, the BGTaskScheduler registrar, the APNs
        // and deep-link bridges). They live here rather than in `:shared`
        // because the composition root must name `firebaseDataModule`, which
        // `:shared` sits below and cannot see.
        iosMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}

dependencies {
    lintChecks(project(":lint"))
}

android {
    namespace = "com.ssbmax.data.firebase"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Matches `:shared` -- see config/detekt/detekt-shared.yml for why the
// "ssbmax" custom rule set activation can't live in the repo-wide config.
detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-shared.yml"))
}
