// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.sqldelight) apply false
    jacoco
}

// Configure Jacoco for all subprojects
subprojects {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.11"
    }

    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}

// Detekt (Kotlin static analysis) for all subprojects.
// Each module keeps its own detekt-baseline.xml so existing issues are
// grandfathered and only NEW smells fail the build.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = file("detekt-baseline.xml")
    }

    // :detekt-rules houses the "ssbmax" custom rule set's sole rule today,
    // HardcodedComposeText -- the commonMain-reaching equivalent of :lint's
    // ComposeHardcodedText (AGP's Android Lint does not analyze a KMP
    // module's commonMain, verified empirically against `shared` during
    // Phase 0c/0h). Scoped to :shared only: `app` already has its own
    // ComposeHardcodedText coverage via AGP Lint (with its own long-standing
    // `lint-baseline.xml`), so applying this there too would just demand a
    // second, redundant baseline for the exact same pre-existing findings.
    //
    // `:data-firebase` is included for the same reason as `:shared`: it is
    // also a KMP module with a commonMain that AGP Lint cannot see. It holds
    // no Compose today, so HardcodedComposeText finds nothing -- wired up now
    // so the coverage exists by default rather than being noticed later.
    if (path in setOf(":shared", ":data-firebase")) {
        dependencies {
            add("detektPlugins", project(":detekt-rules"))
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
        // KMP modules (shared) register generated dirs (Compose Resources
        // accessors, SQLDelight query classes) as their own commonMain source
        // roots, so a glob like "**/build/**" never matches -- it's checked
        // relative to each root, and the root itself IS already inside
        // build/. Matching on the absolute path is the only way that reaches
        // every one of them. Without this, every rule fires on hand-written-
        // looking but fully generated code, and findings churn every time a
        // resource is added or removed (the generated file count shifts).
        exclude { it.file.absolutePath.contains("${File.separator}build${File.separator}") }
        reports {
            sarif.required.set(true)
            html.required.set(true)
            xml.required.set(false)
            txt.required.set(false)
            md.required.set(false)
        }
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "21"
    }

    // `shared` is a Kotlin Multiplatform module: its plain `detekt` task has
    // no `commonMain`/`androidMain`/`iosMain` source configured at all (that
    // lives on the per-target tasks below) and is silently NO-SOURCE, so
    // `./gradlew detekt` -- what CI runs -- was never actually analyzing a
    // single line of `shared` (verified empirically during the Phase 0
    // KMP-convergence plan's 0c/0h work: adding lintChecks/a real rule here
    // found nothing until these were wired in directly). Route `detekt`
    // through the tasks that really have source instead of leaving it to
    // silently report success over zero files.
    //
    // `:data-firebase` (Move 2) is a KMP module too and would inherit the
    // exact same silent NO-SOURCE blind spot, so it gets the same routing.
    if (path in setOf(":shared", ":data-firebase")) {
        tasks.named("detekt") {
            dependsOn("detektMetadataCommonMain", "detektAndroidDebug", "detektIosSimulatorArm64Main")
        }
    }
}
