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
 * CRITICAL: These tests catch the KAPT/KSP processing order issue where
 * Dagger (KSP) runs before AndroidX Hilt (KAPT), causing worker modules
 * to be missing from the generated Dagger component.
 * 
 * Run with: ./gradlew :app:testDebugUnitTest --tests "*.WorkManagerHiltIntegrationTest"
 */
class WorkManagerHiltIntegrationTest {

    private val projectRoot: File = File(System.getProperty("user.dir") ?: ".").parentFile ?: File(".")
    
    // All SSB test workers that require Hilt injection
    private val requiredWorkers = listOf(
        "PPDTAnalysisWorker",
        "WATAnalysisWorker",
        "TATAnalysisWorker",
        "SRTAnalysisWorker",
        "SDTAnalysisWorker",
        "GTOAnalysisWorker",
        "InterviewAnalysisWorker",
        "InterviewQuestionGenerationWorker",
        "QuestionCacheCleanupWorker",
        "ArchivalWorker"
    )

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
        
        // Check for AndroidX hilt compiler (must be kapt, not ksp)
        val hasAndroidXHiltCompiler = buildContent.contains("androidx.hilt:hilt-compiler") &&
            buildContent.contains("kapt(\"androidx.hilt:hilt-compiler")
        
        assertTrue(
            """
            Missing AndroidX Hilt compiler in build.gradle.kts!
            
            When using hilt-work, you MUST add the AndroidX Hilt compiler with KAPT:
            
            kapt("androidx.hilt:hilt-compiler:1.2.0")
            
            This generates the HiltWorkerFactory that injects dependencies into workers.
            """.trimIndent(),
            hasAndroidXHiltCompiler
        )
    }
    
    /**
     * CRITICAL TEST: Prevents KAPT/KSP processing order regression.
     * 
     * If Dagger uses KSP and AndroidX Hilt uses KAPT, the worker modules
     * won't be included in the Dagger component because KSP runs BEFORE KAPT.
     * 
     * Both must use KAPT to ensure proper processing order.
     */
    @Test
    fun `Dagger Hilt must use KAPT not KSP to prevent worker registration failure`() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts not found", buildFile.exists())
        
        val buildContent = buildFile.readText()
        
        // Dagger Hilt compiler MUST use kapt, NOT ksp
        val daggerUsesKapt = buildContent.contains("kapt(libs.hilt.compiler)")
        val daggerUsesKsp = buildContent.contains("ksp(libs.hilt.compiler)")
        
        assertTrue(
            """
            ╔══════════════════════════════════════════════════════════════════════╗
            ║  CRITICAL: Dagger Hilt must use KAPT, not KSP!                       ║
            ╠══════════════════════════════════════════════════════════════════════╣
            ║                                                                      ║
            ║  KSP runs BEFORE KAPT in the build process.                          ║
            ║                                                                      ║
            ║  If Dagger uses KSP and AndroidX Hilt (workers) uses KAPT:           ║
            ║  → Dagger generates component BEFORE worker modules exist            ║
            ║  → Workers won't be registered in HiltWorkerFactory                  ║
            ║  → Runtime error: NoSuchMethodException for all @HiltWorker classes  ║
            ║                                                                      ║
            ║  FIX: Change ksp(libs.hilt.compiler) to kapt(libs.hilt.compiler)     ║
            ╚══════════════════════════════════════════════════════════════════════╝
            """.trimIndent(),
            daggerUsesKapt && !daggerUsesKsp
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
            ?.filter { it.name.endsWith("Worker.kt") }
            ?: emptyList()
        
        assertTrue("No workers found", analysisWorkers.isNotEmpty())
        
        val missingAnnotation = mutableListOf<String>()
        val missingAssistedInject = mutableListOf<String>()
        
        for (worker in analysisWorkers) {
            val content = worker.readText()
            if (!content.contains("@HiltWorker")) {
                missingAnnotation.add(worker.name)
            }
            if (!content.contains("@AssistedInject")) {
                missingAssistedInject.add(worker.name)
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
        
        if (missingAssistedInject.isNotEmpty()) {
            fail(
                """
                These workers are missing @AssistedInject constructor:
                ${missingAssistedInject.joinToString("\n")}
                
                HiltWorker requires @AssistedInject constructor with @Assisted params!
                """.trimIndent()
            )
        }
    }
    
    /**
     * Verify all required SSB test workers exist and are properly configured.
     */
    @Test
    fun `all required SSB workers must exist`() {
        val workersDir = File(projectRoot, "app/src/main/kotlin/com/ssbmax/workers")
        assertTrue("Workers directory not found", workersDir.exists())
        
        val existingWorkers = workersDir.listFiles()
            ?.map { it.nameWithoutExtension }
            ?.toSet() ?: emptySet()
        
        val missingWorkers = requiredWorkers.filter { it !in existingWorkers }
        
        if (missingWorkers.isNotEmpty()) {
            fail(
                """
                Required SSB workers are missing:
                ${missingWorkers.joinToString("\n") { "- $it.kt" }}
                
                These workers are essential for SSB test OLQ analysis.
                """.trimIndent()
            )
        }
    }
    
    /**
     * Verify that test configurations also use KAPT for Hilt.
     */
    @Test
    fun `test Hilt compilers must use KAPT`() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts not found", buildFile.exists())
        
        val buildContent = buildFile.readText()
        
        // Test configuration should use kaptTest, not kspTest
        val hasKspTest = buildContent.contains("kspTest(libs.hilt.compiler)")
        val hasKaptTest = buildContent.contains("kaptTest(libs.hilt.compiler)")
        
        assertTrue(
            "Test Hilt compiler must use kaptTest(libs.hilt.compiler), not kspTest",
            hasKaptTest && !hasKspTest
        )
        
        // AndroidTest configuration should use kaptAndroidTest, not kspAndroidTest
        val hasKspAndroidTest = buildContent.contains("kspAndroidTest(libs.hilt.compiler)")
        val hasKaptAndroidTest = buildContent.contains("kaptAndroidTest(libs.hilt.compiler)")
        
        assertTrue(
            "AndroidTest Hilt compiler must use kaptAndroidTest(libs.hilt.compiler), not kspAndroidTest",
            hasKaptAndroidTest && !hasKspAndroidTest
        )
    }
}
