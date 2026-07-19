import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Phase 0 KMP spike module.
// Ports a single vertical slice (Google sign-in + OIR result fetch) through
// Koin / GitLive-Firebase / SQLDelight / Compose Multiplatform to validate the
// dependency stack before committing to the full migration (see the KMP
// migration plan). Additive — does not replace core:domain/core:data/app.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler.kmp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.common)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // Phase 1 domain move: the 6 MockK-dependent unit tests from core:domain's
        // JVM-only test source set land here (androidUnitTest), not commonTest --
        // MockK has no KMP/native equivalent, so they stay JUnit4 + MockK on the
        // Android target only.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
                // Phase 2: JVM SQLDelight driver to actually exercise CachedOirResult
                // reads/writes against a real in-memory SQLite DB in unit tests --
                // closes the Phase 0 exit report's "SQLDelight unexercised at
                // runtime" gap without needing an emulator/simulator.
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.android)
            // GitLive's Android artifacts declare com.google.firebase:* deps
            // without version pins (expects the consumer to apply Firebase's
            // BOM) -- confirmed via a real build failure ("Could not find
            // com.google.firebase:firebase-auth-ktx:") before this was added.
            implementation(project.dependencies.platform(libs.firebase.bom))
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.ssbmax.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
