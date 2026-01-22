package com.ssbmax.architecture

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Architecture tests to prevent WorkManager + Hilt integration regressions.
 * 
 * These tests verify that critical configuration is present in build files
 * and manifest, without needing to run the full Android app.
 * 
 * Run with: ./gradlew :app:test --tests "*.WorkManagerHiltIntegrationTest"
 */
class WorkManagerHiltIntegrationTest {

    private val projectRoot: File = File(System.getProperty("user.dir") ?: ".").parentFile ?: File(".")

    @Test
    fun `AndroidManifest should disable default WorkManager initialization`() {
        val manifestFile = File(projectRoot, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml not found", manifestFile.exists())
        
        val manifestContent = manifestFile.readText()
        
        // Check for the InitializationProvider that disables default WorkManager
        val hasInitializationProvider = manifestContent.contains("androidx.startup.InitializationProvider")
        val hasWorkManagerInitializerRemoval = manifestContent.contains("androidx.work.WorkManagerInitializer") &&
            manifestContent.contains("tools:node=\"remove\"")
        
        assertTrue(
            """
            AndroidManifest.xml must disable default WorkManager initialization!
            
            This is CRITICAL for Hilt workers to receive dependency injection.
            
            Add this to AndroidManifest.xml inside <application>:
            
            <provider
                android:name="androidx.startup.InitializationProvider"
                android:authorities="${'$'}{applicationId}.androidx-startup"
                android:exported="false"
                tools:node="merge">
                <meta-data
                    android:name="androidx.work.WorkManagerInitializer"
                    android:value="androidx.startup"
                    tools:node="remove" />
            </provider>
            """.trimIndent(),
            hasInitializationProvider && hasWorkManagerInitializerRemoval
        )
    }

    @Test
    fun `build gradle should have AndroidX Hilt compiler for WorkManager`() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts not found", buildFile.exists())
        
        val buildContent = buildFile.readText()
        
        // Check for hilt-work implementation
        val hasHiltWork = buildContent.contains("androidx.hilt:hilt-work")
        assertTrue(
            "Missing implementation(\"androidx.hilt:hilt-work:x.x.x\") in build.gradle.kts",
            hasHiltWork
        )
        
        // Check for AndroidX hilt compiler (either ksp or kapt)
        val hasAndroidXHiltCompiler = buildContent.contains("androidx.hilt:hilt-compiler") &&
            (buildContent.contains("ksp(\"androidx.hilt:hilt-compiler") || 
             buildContent.contains("kapt(\"androidx.hilt:hilt-compiler"))
        
        assertTrue(
            """
            Missing AndroidX Hilt compiler in build.gradle.kts!
            
            When using hilt-work, you MUST add the AndroidX Hilt compiler:
            
            ksp("androidx.hilt:hilt-compiler:1.2.0")
            
            This generates the HiltWorkerFactory that injects dependencies into workers.
            """.trimIndent(),
            hasAndroidXHiltCompiler
        )
    }

    @Test
    fun `Application class should implement Configuration Provider for WorkManager`() {
        val appFile = File(projectRoot, "app/src/main/kotlin/com/ssbmax/SSBMaxApplication.kt")
        assertTrue("SSBMaxApplication.kt not found", appFile.exists())
        
        val appContent = appFile.readText()
        
        assertTrue(
            "SSBMaxApplication must implement Configuration.Provider interface",
            appContent.contains("Configuration.Provider")
        )
        
        assertTrue(
            "SSBMaxApplication must inject HiltWorkerFactory",
            appContent.contains("HiltWorkerFactory")
        )
        
        assertTrue(
            "SSBMaxApplication must override workManagerConfiguration",
            appContent.contains("workManagerConfiguration")
        )
    }

    @Test
    fun `all analysis workers should have HiltWorker annotation`() {
        val workersDir = File(projectRoot, "app/src/main/kotlin/com/ssbmax/workers")
        assertTrue("Workers directory not found", workersDir.exists())
        
        val analysisWorkers = workersDir.listFiles()
            ?.filter { it.name.endsWith("AnalysisWorker.kt") }
            ?: emptyList()
        
        assertTrue("No analysis workers found", analysisWorkers.isNotEmpty())
        
        val missingAnnotation = mutableListOf<String>()
        
        for (worker in analysisWorkers) {
            val content = worker.readText()
            if (!content.contains("@HiltWorker")) {
                missingAnnotation.add(worker.name)
            }
        }
        
        if (missingAnnotation.isNotEmpty()) {
            fail(
                """
                These workers are missing @HiltWorker annotation:
                ${missingAnnotation.joinToString("\n")}
                
                Without @HiltWorker, dependencies won't be injected!
                """.trimIndent()
            )
        }
    }
}
