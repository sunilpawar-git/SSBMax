pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SSBMax"
include(":app")
include(":lint")
include(":detekt-rules")

// Phase 1 KMP migration (see docs/architecture/ or CLAUDE plan for context):
// core:domain was fully moved into shared/commonMain/domain and the old
// module deleted. KMP-convergence Phase 9f: core:data deleted too — its one
// surviving live piece (the TAT Room cache) moved into `app`, everything
// else had zero production callers left. `shared` is now the SSOT data/UI/
// navigation module for both platforms; `app` is Android platform glue only.
include(":shared")

// Move 2 of the iOS CocoaPods->SPM convergence: the Firebase-backed
// implementations of `:shared`'s repository interfaces, extracted so that
// `:shared` itself carries no Firebase in its klib graph and its
// Kotlin/Native test binaries link without CocoaPods. Sits ABOVE `:shared`
// (depends on it, not vice versa) and is consumed only by the app binaries.
// See data-firebase/build.gradle.kts for the linker facts behind the split.
include(":data-firebase")
