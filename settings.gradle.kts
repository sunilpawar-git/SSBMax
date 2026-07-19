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
include(":core:common")
include(":core:designsystem")
include(":core:data")
include(":lint")

// Phase 1 KMP migration (see docs/architecture/ or CLAUDE plan for context):
// core:domain was fully moved into shared/commonMain/domain and the old
// module deleted. `shared` is now a required dependency (app, core:data),
// not an additive/parallel one.
include(":shared")
