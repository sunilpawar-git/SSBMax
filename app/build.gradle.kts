import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import java.math.BigDecimal
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Temporarily disable mock google-services.json generation for local development
// Uncomment the block below if you need CI builds without Firebase config

/*
tasks.register("createMockGoogleServices") {
    doLast {
        if (!file("google-services.json").exists()) {
            println("⚠️ google-services.json not found - creating mock for CI build")
            val mockGoogleServices = """
            {
              "project_info": {
                "project_number": "123456789",
                "project_id": "mock-project-id",
                "storage_bucket": "mock-project-id.appspot.com"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "1:123456789:android:mockappid",
                    "android_client_info": {
                      "package_name": "com.ssbmax"
                    }
                  },
                  "oauth_client": [],
                  "api_key": [
                    {
                      "current_key": "mock_api_key_for_ci"
                    }
                  ],
                  "services": {
                    "appinvite_service": {
                      "other_platform_oauth_client": []
                    }
                  }
                }
              ],
              "configuration_version": "1"
            }
            """.trimIndent()

            file("google-services.json").writeText(mockGoogleServices)
            println("✅ Created mock google-services.json for CI build with package name 'com.ssbmax'")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("createMockGoogleServices")
}
*/

extensions.getByType<ApplicationExtension>().apply {
    namespace = "com.ssbmax"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssbmax"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.ssbmax.testing.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Gemini API Key for AI Interview Feature
        // Read from local.properties (fallback to project property, then empty string)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
            ?: project.findProperty("GEMINI_API_KEY") as? String
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

    }

    buildTypes {
        debug {
            isDebuggable = true
            // TODO: Enable after adding com.ssbmax.debug to Firebase Console
            // applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            // Debug bypass for subscription limits during development
            // Applies to ALL tests: OIR, PPDT, WAT, SRT, TAT, GTO, Self Description, Interview
            // ENABLED FOR DEVELOPMENT - DISABLE TO TEST SUBSCRIPTION FLOW
            buildConfigField("boolean", "BYPASS_SUBSCRIPTION_LIMITS", "true")
            
            // Debug: Bypass interview prerequisites (PIQ, OIR score >= 50%, PPDT)
            // Set to "true" to bypass all prerequisite checks for testing TTS and interview features
            // ENABLED FOR DEVELOPMENT - DISABLE TO TEST PREREQUISITE FLOW
            buildConfigField("boolean", "BYPASS_INTERVIEW_PREREQUISITES", "true")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Production: Subscription limits enforced
            buildConfigField("boolean", "BYPASS_SUBSCRIPTION_LIMITS", "false")
            
            // Production: Prerequisites enforced
            buildConfigField("boolean", "BYPASS_INTERVIEW_PREREQUISITES", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    lint {
        // Enforce string resources - fail build on hardcoded text
        error += "HardcodedText"
        lintConfig = file("lint.xml")

        // Lint baseline for systematic cleanup of warnings (Phase 1.5)
        baseline = file("lint-baseline.xml")

        // Fail build on any errors (enforce quality)
        abortOnError = false
        warningsAsErrors = false // Can enable later for stricter enforcement

        disable += setOf(
            "ModifierParameter",
            "ModifierDeclaration",
            "ModifierFactoryExtensionFunction",
            "ModifierFactoryReturnType",
            "ModifierFactoryUnreferencedReceiver"
        )
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            
            all {
                // Global timeout: 60 seconds per test to prevent hangs
                it.systemProperty("junit.jupiter.execution.timeout.default", "60s")
                
                // JUnit 4 timeout (for older tests)
                it.jvmArgs("-Djunit.timeout=60000")
                
                // Fail fast - stop on first failure
                it.failFast = true
                
                // Max heap size
                it.maxHeapSize = "2g"
            }
        }
    }
}


dependencies {
    // Core modules
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    // Custom lint rules
    lintChecks(project(":lint"))
    
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("androidx.compose.material:material:1.6.0") // For pull-to-refresh
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Hilt - Use KAPT for both Dagger and AndroidX Hilt to ensure proper processing order
    // KSP runs before KAPT, so if Dagger uses KSP, worker modules won't be included
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)  // Dagger Hilt compiler
    implementation(libs.hilt.navigation.compose)

    // WorkManager (background jobs for question pre-generation)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")  // AndroidX Hilt compiler for @HiltWorker

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    
    // Google Sign-In
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Image Loading
    implementation(libs.coil.compose)


    // Google Play Billing
    implementation(libs.billing.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.11.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(libs.hilt.android.testing)
    kaptTest(libs.hilt.compiler)

    // WorkManager testing (for Phase 1 worker tests)
    testImplementation("androidx.work:work-testing:2.9.0")

    // Robolectric for Android unit tests
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android) // For mocking in UI tests
    androidTestImplementation(libs.androidx.navigation.testing) // For navigation testing
    kaptAndroidTest(libs.hilt.compiler)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Memory leak detection (debug builds only)
    debugImplementation(libs.leakcanary.android)
}

// Jacoco code coverage configuration
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Debug build"
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(
            // Android generated
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",

            // Hilt/Dagger generated
            "**/*_MembersInjector.class",
            "**/Dagger*Component*.*",
            "**/*Module_*Factory.class",
            "**/di/**",
            "**/*_Factory*.*",
            "**/*_Impl*.*",
            "**/HiltWrapper*.*",
            "**/*_Hilt*.*",

            // Navigation generated
            "**/*Navigation*.*",
            "**/*Destinations*.*",
            "**/*NavGraph*.*",

            // Application class
            "**/*Application*.*",

            // Theme/Design system (UI only, no logic)
            "**/ui/theme/**",
            "**/designsystem/**",

            // Lambda classes
            "**/Lambda$*.class",
            "**/Lambda.class",
            "**/*Lambda.class",
            "**/*Lambda*.class"
        )
    }
    
    val mainSrc = "${project.projectDir}/src/main/kotlin"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec", "**/testDebugUnitTest.exec", "**/testDebugUnitTest.ec")
    })
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")
    group = "Verification"
    description = "Validate jacoco coverage for Debug unit tests"

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*_MembersInjector.class",
            "**/Dagger*Component*.*",
            "**/*Module_*Factory.class",
            "**/di/**",
            "**/*_Factory*.*",
            "**/*_Impl*.*",
            "**/HiltWrapper*.*",
            "**/*_Hilt*.*",
            "**/*Navigation*.*",
            "**/*Destinations*.*",
            "**/*NavGraph*.*",
            "**/*Application*.*",
            "**/ui/theme/**",
            "**/designsystem/**",
            "**/Lambda$*.class",
            "**/Lambda.class",
            "**/*Lambda.class",
            "**/*Lambda*.class"
        )
    }

    val mainSrc = "${project.projectDir}/src/main/kotlin"

    classDirectories.setFrom(files(debugTree))
    sourceDirectories.setFrom(files(mainSrc))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec", "**/testDebugUnitTest.exec", "**/testDebugUnitTest.ec")
    })

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.03")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.01")
            }
        }
    }

    onlyIf { executionData.files.any { it.exists() } }
}

tasks.named("check") {
    // Coverage verification can be run explicitly in CI (not wired here to avoid extra task ordering)
}
