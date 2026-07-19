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
include(":core:domain")
include(":core:data")
include(":lint")

// Phase 0 KMP spike (see docs/architecture/ or CLAUDE plan for context).
// Additive/parallel to existing modules — does not replace them yet.
include(":shared")
