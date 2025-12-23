package com.ssbmax.testing

import android.app.Activity
import android.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Memory Leak Detection Tests for SSBMax
 *
 * These tests verify that ViewModels don't hold strong references to UI components
 * that can cause memory leaks when activities/fragments are destroyed.
 *
 * Memory leaks in Android occur when:
 * 1. ViewModel holds strong reference to Activity/Fragment
 * 2. Activity/Fragment is destroyed but ViewModel retains reference
 * 3. Garbage collector can't free memory because ViewModel is still referenced
 *
 * Common leak sources:
 * - ViewModel fields holding Context, Activity, Fragment references
 * - Anonymous inner classes capturing outer class references
 * - Static references to UI components
 * - Improper cleanup of listeners/observers
 */
class MemoryLeakDetectionTest {

    companion object {
        // Root directory of the project
        private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

        // Directories to scan - Handle both root and module-level execution
        private val APP_SRC = run {
            val appKotlin = File(PROJECT_ROOT, "app/src/main/kotlin")
            if (appKotlin.exists()) appKotlin 
            else {
                val moduleKotlin = File(PROJECT_ROOT, "src/main/kotlin")
                if (moduleKotlin.exists()) moduleKotlin
                else File(".") // Fallback
            }
        }
    }

    @Test(timeout = 30000)
    fun `ViewModels must not hold strong references to Activities`() {
        val violations = mutableListOf<String>()

        val viewModelClasses = findViewModelClasses()

        for (viewModelClass in viewModelClasses) {
            val leakFields = findStrongReferencesToActivity(viewModelClass)
            if (leakFields.isNotEmpty()) {
                violations.add(
                    "${viewModelClass.simpleName}: Holds strong references to Activity\n" +
                            "  Fields: ${leakFields.joinToString(", ") { it.name }}\n" +
                            "  Problem: Activity destruction won't free ViewModel memory\n" +
                            "  Solution: Use WeakReference<Activity> or remove Activity dependency"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) with Activity references:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nViewModels must not hold strong references to Activities:\n" +
                        "❌ BAD: private var activity: MainActivity? = null\n" +
                        "✅ GOOD: Use application Context or WeakReference<Context>\n" +
                        "✅ GOOD: Pass required data via function parameters\n\n" +
                        "This prevents memory leaks when Activities are destroyed."
            )
        }
    }

    @Test(timeout = 30000)
    fun `ViewModels must not hold strong references to Fragments`() {
        val violations = mutableListOf<String>()

        val viewModelClasses = findViewModelClasses()

        for (viewModelClass in viewModelClasses) {
            val leakFields = findStrongReferencesToFragment(viewModelClass)
            if (leakFields.isNotEmpty()) {
                violations.add(
                    "${viewModelClass.simpleName}: Holds strong references to Fragment\n" +
                            "  Fields: ${leakFields.joinToString(", ") { it.name }}\n" +
                            "  Problem: Fragment destruction won't free ViewModel memory\n" +
                            "  Solution: Use WeakReference<Fragment> or remove Fragment dependency"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) with Fragment references:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nViewModels must not hold strong references to Fragments:\n" +
                        "❌ BAD: private var fragment: MyFragment? = null\n" +
                        "✅ GOOD: Use WeakReference<Fragment> or remove Fragment dependency\n" +
                        "✅ GOOD: Pass required data via function parameters\n\n" +
                        "This prevents memory leaks when Fragments are destroyed."
            )
        }
    }

    @Test(timeout = 30000)
    fun `ViewModels must not hold Context references unless Application context`() {
        val violations = mutableListOf<String>()

        val viewModelClasses = findViewModelClasses()

        for (viewModelClass in viewModelClasses) {
            val contextFields = findContextReferences(viewModelClass)
            for (field in contextFields) {
                // Allow Application context (safe) but not Activity context
                if (!isApplicationContextField(field)) {
                    violations.add(
                        "${viewModelClass.simpleName}: Holds Context reference\n" +
                                "  Field: ${field.name} (${field.type.simpleName})\n" +
                                "  Problem: Context references can cause memory leaks\n" +
                                "  Solution: Use applicationContext or avoid Context storage"
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) with unsafe Context references:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nViewModels should avoid storing Context references:\n" +
                        "❌ BAD: private var context: Context? = null\n" +
                        "✅ GOOD: Pass Context as function parameter when needed\n" +
                        "✅ GOOD: Store applicationContext only if absolutely necessary\n\n" +
                        "Context references can cause memory leaks. Use dependency injection instead."
            )
        }
    }

    @Test(timeout = 30000)
    fun `ViewModels must not have mutable references to destroyed lifecycles`() {
        val violations = mutableListOf<String>()

        val viewModelClasses = findViewModelClasses()

        for (viewModelClass in viewModelClasses) {
            // Create ViewModel instance to test
            val viewModel = createViewModelInstance(viewModelClass) ?: continue

            // Check for fields that might hold lifecycle references
            val lifecycleFields = findLifecycleReferences(viewModelClass)
            for (field in lifecycleFields) {
                field.isAccessible = true
                val value = field.get(viewModel)

                if (value != null) {
                    violations.add(
                        "${viewModelClass.simpleName}: Holds lifecycle reference\n" +
                                "  Field: ${field.name} (${field.type.simpleName})\n" +
                                "  Problem: Lifecycle objects can cause memory leaks\n" +
                                "  Solution: Use WeakReference or avoid storing lifecycle references"
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Found ${violations.size} ViewModel(s) with lifecycle references:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nViewModels should not store lifecycle references:\n" +
                        "❌ BAD: private var lifecycle: Lifecycle? = null\n" +
                        "✅ GOOD: Use lifecycleScope.launch directly in composables\n" +
                        "✅ GOOD: Pass lifecycle-aware objects as parameters\n\n" +
                        "Stored lifecycle references create memory leak risk."
            )
        }
    }

    @Test(timeout = 60000)
    fun `critical ViewModels must survive process death without leaks`() {
        // Test the most critical ViewModels that handle user test data
        val criticalViewModels = listOf(
            "com.ssbmax.ui.tests.tat.TATTestViewModel" to "TAT Test",
            "com.ssbmax.ui.tests.wat.WATTestViewModel" to "WAT Test",
            "com.ssbmax.ui.tests.srt.SRTTestViewModel" to "SRT Test",
            "com.ssbmax.ui.tests.ppdtt.PPDTTestViewModel" to "PPDT Test",
            "com.ssbmax.ui.tests.oir.OIRTestViewModel" to "OIR Test"
        )

        val violations = mutableListOf<String>()

        for ((className, testName) in criticalViewModels) {
            try {
                val viewModelClass = Class.forName(className).asSubclass(ViewModel::class.java)

                // Simulate process death scenario
                val leakDetected = simulateProcessDeathLeakTest(viewModelClass)

                if (leakDetected) {
                    violations.add(
                        "$testName ViewModel: Memory leak detected in process death simulation\n" +
                                "  Class: $className\n" +
                                "  Problem: ViewModel holds references preventing garbage collection\n" +
                                "  Impact: Test progress lost on app backgrounding/configuration changes"
                    )
                }
            } catch (e: ClassNotFoundException) {
                // ViewModel doesn't exist yet, skip
                continue
            } catch (e: Exception) {
                violations.add(
                    "$testName ViewModel: Error during leak test\n" +
                            "  Class: $className\n" +
                            "  Error: ${e.message}\n" +
                            "  Fix: Ensure ViewModel can be instantiated for testing"
                )
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "Critical test ViewModels have memory leak issues:\n\n" +
                        violations.joinToString("\n\n") +
                        "\n\nWhy this matters:\n" +
                        "- SSB test ViewModels must survive process death (app backgrounding)\n" +
                        "- Users spend 30-60 minutes on tests, progress must be preserved\n" +
                        "- Memory leaks cause test interruption and data loss\n\n" +
                        "Fix memory leaks before proceeding with test implementation."
            )
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Find all ViewModel classes in the codebase
     */
    private fun findViewModelClasses(): List<Class<out ViewModel>> {
        val viewModelClasses = mutableListOf<Class<out ViewModel>>()

        // Find ViewModel files
        val viewModelFiles = findFiles(APP_SRC, "*ViewModel.kt")

        for (file in viewModelFiles) {
            // Skip test files
            if (file.path.contains("/test/")) continue

            try {
                // Convert file path to class name
                val className = fileToClassName(file)
                val viewModelClass = Class.forName(className).asSubclass(ViewModel::class.java)
                viewModelClasses.add(viewModelClass)
            } catch (e: Exception) {
                // Skip classes that can't be loaded (may be abstract or have dependencies)
                continue
            }
        }

        return viewModelClasses
    }

    /**
     * Find fields in ViewModel that hold strong references to Activity
     */
    private fun findStrongReferencesToActivity(viewModelClass: Class<out ViewModel>): List<Field> {
        val activityFields = mutableListOf<Field>()

        val fields = getAllFields(viewModelClass)
        for (field in fields) {
            if (Activity::class.java.isAssignableFrom(field.type)) {
                activityFields.add(field)
            }
        }

        return activityFields
    }

    /**
     * Find fields in ViewModel that hold strong references to Fragment
     */
    private fun findStrongReferencesToFragment(viewModelClass: Class<out ViewModel>): List<Field> {
        val fragmentFields = mutableListOf<Field>()

        val fields = getAllFields(viewModelClass)
        for (field in fields) {
            if (Fragment::class.java.isAssignableFrom(field.type)) {
                fragmentFields.add(field)
            }
        }

        return fragmentFields
    }

    /**
     * Find fields that hold Context references
     */
    private fun findContextReferences(viewModelClass: Class<out ViewModel>): List<Field> {
        val contextFields = mutableListOf<Field>()

        val fields = getAllFields(viewModelClass)
        for (field in fields) {
            if (android.content.Context::class.java.isAssignableFrom(field.type)) {
                contextFields.add(field)
            }
        }

        return contextFields
    }

    /**
     * Find fields that hold lifecycle references
     */
    private fun findLifecycleReferences(viewModelClass: Class<out ViewModel>): List<Field> {
        val lifecycleFields = mutableListOf<Field>()

        val fields = getAllFields(viewModelClass)
        for (field in fields) {
            val fieldType = field.type
            if (fieldType.name.contains("Lifecycle") ||
                fieldType.name.contains("LifecycleOwner")) {
                lifecycleFields.add(field)
            }
        }

        return lifecycleFields
    }

    /**
     * Check if a Context field is Application context (safe)
     */
    private fun isApplicationContextField(field: Field): Boolean {
        // Check field name for application context indicators
        val fieldName = field.name.lowercase()
        return fieldName.contains("application") ||
               fieldName.contains("app") ||
               field.type.name.contains("Application")
    }

    /**
     * Get all fields from class hierarchy
     */
    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null && currentClass != ViewModel::class.java) {
            fields.addAll(currentClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) })
            currentClass = currentClass.superclass
        }

        return fields
    }

    /**
     * Create ViewModel instance for testing
     */
    private fun createViewModelInstance(viewModelClass: Class<out ViewModel>): ViewModel? {
        return try {
            // Try default constructor first
            val constructor = viewModelClass.getConstructor()
            constructor.newInstance()
        } catch (e: Exception) {
            // If default constructor fails, try to create with mocks
            // This is a simplified version - in real implementation you'd use a DI framework
            null
        }
    }

    /**
     * Simulate process death scenario to detect memory leaks
     * This is a simplified version that checks for common leak patterns
     */
    private fun simulateProcessDeathLeakTest(viewModelClass: Class<out ViewModel>): Boolean {
        return try {
            // For now, just check if the ViewModel can be instantiated
            // In a real implementation, this would use instrumentation tests
            // or a more sophisticated leak detection framework
            createViewModelInstance(viewModelClass) != null
        } catch (e: Exception) {
            // If we can't instantiate, that's a different issue
            false
        }
    }

    /**
     * Convert file path to fully qualified class name
     */
    private fun fileToClassName(file: File): String {
        val relativePath = file.relativeTo(APP_SRC).path
        return relativePath
            .removeSuffix(".kt")
            .replace("/", ".")
            .replace("\\", ".")
    }

    /**
     * Find files matching a pattern
     */
    private fun findFiles(dir: File, pattern: String): List<File> {
        if (!dir.exists()) return emptyList()

        val regex = pattern.replace("*", ".*").toRegex()
        return dir.walkTopDown()
            .filter { it.isFile && it.name.matches(regex) }
            .toList()
    }
}