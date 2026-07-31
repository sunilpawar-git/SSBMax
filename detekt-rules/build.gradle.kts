plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:${libs.versions.detekt.get()}")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-api:${libs.versions.detekt.get()}")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:${libs.versions.detekt.get()}")
    testImplementation(kotlin("test"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
