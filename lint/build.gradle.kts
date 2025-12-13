plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Lint API dependencies
    compileOnly("com.android.tools.lint:lint-api:31.13.2")
    compileOnly("com.android.tools.lint:lint-checks:31.13.2")

    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.android.tools.lint:lint:31.13.2")
    testImplementation("com.android.tools.lint:lint-tests:31.13.2")
    testImplementation("com.android.tools:testutils:31.5.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "com.ssbmax.lint.SSBMaxIssueRegistry"
        )
    }
}
