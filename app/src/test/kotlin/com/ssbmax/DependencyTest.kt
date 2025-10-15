package com.ssbmax

import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify modern dependencies are loaded correctly
 */
class DependencyTest {
    
    @Test
    fun compose_dependencies_available() {
        // Verify Compose classes are on classpath
        assertNotNull(androidx.compose.runtime.Composable::class.java)
        assertNotNull(androidx.compose.material3.MaterialTheme::class.java)
        assertNotNull(androidx.compose.ui.Modifier::class.java)
    }
    
    @Test
    fun androidx_lifecycle_available() {
        // Verify Lifecycle components are available
        assertNotNull(androidx.lifecycle.ViewModel::class.java)
        assertNotNull(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner::class.java)
    }
    
    @Test
    fun coroutines_available() {
        // Verify Coroutines are available
        assertNotNull(kotlinx.coroutines.CoroutineScope::class.java)
        assertNotNull(kotlinx.coroutines.flow.Flow::class.java)
        assertNotNull(kotlinx.coroutines.flow.StateFlow::class.java)
    }
    
    @Test
    fun kotlin_stdlib_available() {
        // Verify Kotlin stdlib
        val list = listOf(1, 2, 3)
        assertEquals(3, list.size)
    }
}
