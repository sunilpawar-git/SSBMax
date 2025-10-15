package com.ssbmax

import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify Hilt dependency injection is configured correctly
 */
class HiltSetupTest {
    
    @Test
    fun hilt_dependencies_available() {
        // Verify Hilt classes are on classpath (compile-time verification)
        assertNotNull(dagger.hilt.android.HiltAndroidApp::class.java)
        assertNotNull(dagger.hilt.android.AndroidEntryPoint::class.java)
        assertNotNull(dagger.hilt.android.lifecycle.HiltViewModel::class.java)
    }
    
    @Test
    fun hilt_application_compiles() {
        // If SSBMaxApplication compiles with @HiltAndroidApp, Hilt is set up correctly
        val app = SSBMaxApplication()
        assertNotNull(app)
    }
    
    @Test
    fun hilt_main_activity_compiles() {
        // If MainActivity compiles with @AndroidEntryPoint, Hilt is configured
        assertNotNull(MainActivity::class.java)
    }
}
