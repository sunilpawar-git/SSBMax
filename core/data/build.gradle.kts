import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import java.math.BigDecimal

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("jacoco")
}

android {
    namespace = "com.ssbmax.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "com.ssbmax.core.data.FirebaseTestRunner"
        consumerProguardFiles("consumer-rules.pro")

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

        // Room schema export configuration
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            // Enable BuildConfig for DEBUG flag checks
            buildConfigField("boolean", "DEBUG", "true")
            // Development: Use direct Gemini API calls (faster iteration)
            buildConfigField("boolean", "USE_CLOUD_AI", "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEBUG", "false")
            // Production: Use Firebase Cloud Functions (secure, no exposed API key)
            buildConfigField("boolean", "USE_CLOUD_AI", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
    
    kotlin {
        jvmToolchain(21)
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test> {
    systemProperty("firebase.emulator.host", "localhost")
    systemProperty("firestore.emulator.host", "localhost:8080")
    systemProperty("auth.emulator.host", "localhost:9099")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":core:domain"))
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.functions)
    
    // Google Sign-In
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // Gemini AI
    implementation(libs.generativeai)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Gson for JSON serialization
    implementation(libs.gson)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine) // For Flow testing
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation(libs.room.testing)
    
    // Android instrumented tests (integration tests with Firebase Emulator)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine) // For Flow testing
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.firestore) // For emulator tests
    androidTestImplementation(libs.firebase.auth) // For auth emulator tests
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
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*_Factory*.*",
            "**/*_Impl*.*",
            "**/*_Hilt*.*",
            "**/hilt/**",
            "**/di/**"
        )
    }

    sourceDirectories.setFrom(files("${project.projectDir}/src/main/kotlin"))
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
            "**/*_Factory*.*",
            "**/*_Impl*.*",
            "**/*_Hilt*.*",
            "**/hilt/**",
            "**/di/**"
        )
    }

    classDirectories.setFrom(files(debugTree))
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/kotlin"))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec", "**/testDebugUnitTest.exec", "**/testDebugUnitTest.ec")
    })

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.05")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.03")
            }
        }
    }

    onlyIf { executionData.files.any { it.exists() } }
}

tasks.named("check") {
    // Coverage verification can be run explicitly in CI (not wired here to avoid extra task ordering)
}

