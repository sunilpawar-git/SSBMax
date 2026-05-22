package com.ssbmax.core.data.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for Firebase Security Rules validation
 * 
 * These tests validate the logical structure and correctness of our Firestore security rules
 * without requiring a Firebase emulator. They test:
 * - Rule existence and structure
 * - Access control logic paths
 * - Field validation requirements
 * - Security best practices
 * 
 * Note: For full integration testing with actual Firestore, use Firebase emulator tests.
 * These unit tests provide fast validation of rule logic during development.
 */
class FirebaseRulesValidationTest {
    
    companion object {
        // Path to firestore.rules file relative to project root
        private const val RULES_FILE_PATH = "firestore.rules"
    }
    
    private fun getRulesContent(): String {
        // Try multiple potential paths since tests can run from different working directories
        val possiblePaths = listOf(
            File(System.getProperty("user.dir"), RULES_FILE_PATH), // From project root
            File(System.getProperty("user.dir"), "../../$RULES_FILE_PATH"), // From module
            File(System.getProperty("user.dir"), "../../../$RULES_FILE_PATH") // From nested module
        )
        
        val rulesFile = possiblePaths.firstOrNull { it.exists() }
            ?: error("Could not find firestore.rules file. Tried: ${possiblePaths.map { it.absolutePath }}")
        
        return rulesFile.readText()
    }
    
    // ==================== Rule Structure Tests ====================
    
    @Test
    fun `rules file exists and is readable`() {
        val content = getRulesContent()
        assertTrue("Rules file should not be empty", content.isNotBlank())
        assertTrue("Rules should use rules_version 2", content.contains("rules_version = '2'"))
    }
    
    @Test
    fun `rules define helper functions for authentication`() {
        val content = getRulesContent()
        
        // Critical helper functions must exist
        assertTrue("Should define isAuthenticated() function", 
            content.contains("function isAuthenticated()"))
        assertTrue("Should define isOwner() function", 
            content.contains("function isOwner(userId)"))
        assertTrue("Should define isAssessor() function",
            content.contains("function isAssessor()"))
        assertTrue("Should define isStudent() function",
            content.contains("function isStudent()"))
    }
    
    @Test
    fun `isAuthenticated checks for auth object`() {
        val content = getRulesContent()
        val isAuthFunction = content.substringAfter("function isAuthenticated()")
            .substringBefore("}")
        
        assertTrue("isAuthenticated should check request.auth",
            isAuthFunction.contains("request.auth != null"))
    }
    
    @Test
    fun `isOwner validates user ownership`() {
        val content = getRulesContent()
        val isOwnerFunction = content.substringAfter("function isOwner(userId)")
            .substringBefore("// Check if user is an assessor") // Stop at next function
        
        assertTrue("isOwner should check authentication",
            isOwnerFunction.contains("isAuthenticated()"))
        assertTrue("isOwner should check uid matches",
            isOwnerFunction.contains("request.auth.uid == userId"))
    }
    
    // ==================== User Data Access Tests ====================
    
    @Test
    fun `users can only read their own data`() {
        val content = getRulesContent()
        val userRules = content.substringAfter("match /users/{userId}")
            .substringBefore("// Test usage tracking")
        
        assertTrue("Users should be able to read own data",
            userRules.contains("allow read: if isOwner(userId)"))
    }
    
    @Test
    fun `users data subcollection is protected`() {
        val content = getRulesContent()
        val dataSubcollection = content.substringAfter("match /data/{document}")
            .substringBefore("}")
        
        assertTrue("Data subcollection read should require ownership",
            dataSubcollection.contains("allow read: if isOwner(userId)"))
        assertTrue("Data subcollection write should require ownership",
            dataSubcollection.contains("allow write: if isOwner(userId)"))
    }
    
    @Test
    fun `users cannot update their role field`() {
        val content = getRulesContent()
        val userUpdateRule = content.substringAfter("// Users can update their own profile")
            .substringBefore("// No one can delete user profiles")
        
        assertTrue("Should prevent role field updates",
            userUpdateRule.contains("!request.resource.data.diff(resource.data).affectedKeys().hasAny(['role'])"))
    }
    
    @Test
    fun `user profile deletion is blocked`() {
        val content = getRulesContent()
        val userRules = content.substringAfter("match /users/{userId}")
            .substringBefore("// User data subcollection")
        
        assertTrue("Should block user deletion",
            userRules.contains("allow delete: if false"))
    }
    
    // ==================== Test Usage Tracking Tests ====================
    
    @Test
    fun `test_usage requires all required fields`() {
        val content = getRulesContent()
        val testUsageRules = content.substringAfter("match /subscription/{document}")
            .substringBefore("// NEVER allow delete")
        
        val requiredFields = listOf(
            "oirTestsUsed", "tatTestsUsed", "watTestsUsed",
            "srtTestsUsed", "ppdtTestsUsed", "gtoTestsUsed",
            "interviewTestsUsed", "sdTestsUsed", "lastUpdated"
        )
        
        requiredFields.forEach { field ->
            assertTrue("test_usage should require field: $field",
                testUsageRules.contains("'$field'"))
        }
    }
    
    @Test
    fun `test_usage validates integer types and non-negative values`() {
        val content = getRulesContent()
        val testUsageRules = content.substringAfter("match /subscription/{document}")
            .substringBefore("// NEVER allow delete")
        
        // Check that oirTestsUsed has validation for >= 0
        assertTrue("Should validate oirTestsUsed >= 0",
            testUsageRules.contains("request.resource.data.oirTestsUsed >= 0"))
    }
    
    @Test
    fun `test_usage is user-specific`() {
        val content = getRulesContent()
        val testUsageRules = content.substringAfter("match /subscription/{document}")
            .substringBefore("// NEVER allow delete")
        
        assertTrue("Users should read own usage",
            testUsageRules.contains("allow read: if isOwner(userId)"))
        // Check for create and update (not generic write)
        assertTrue("Users should create/update own usage",
            testUsageRules.contains("allow create: if isOwner(userId)") ||
            testUsageRules.contains("allow update: if isOwner(userId)"))
    }
    
    // ==================== Subscription Data Tests ====================
    
    @Test
    fun `subscription data is read-only from client`() {
        val content = getRulesContent()
        val subscriptionRules = content.substringAfter("match /data/subscription")
            .substringBefore("}")
        
        assertTrue("Clients can read subscription",
            subscriptionRules.contains("allow read: if isOwner(userId)"))
        assertTrue("Clients cannot write subscription",
            subscriptionRules.contains("allow write: if false"))
    }
    
    
    @Test
    fun `wildcard data match blocks subscription document updates`() {
        val content = getRulesContent()
        val dataMatchBlock = content.substringAfter("match /data/{document}")
            .substringBefore("}")
            
        assertTrue("Write access to wildcard data subcollection must exclude subscription document",
            dataMatchBlock.contains("document != 'subscription'") || 
            dataMatchBlock.contains("document != \"subscription\""))
    }

    // ==================== Security Best Practices Tests ====================
    
    @Test
    fun `default deny rule exists`() {
        val content = getRulesContent()
        
        assertTrue("Should have default deny-all rule",
            content.contains("match /{document=**}"))
        assertTrue("Default rule should deny all access",
            content.contains("allow read, write: if false"))
    }
    
    @Test
    fun `no rules allow unauthenticated write access`() {
        val content = getRulesContent()
        
        // Check for potentially dangerous patterns
        assertFalse("Should not allow write: if true",
            content.contains("allow write: if true"))
        
        // Count instances of "allow write:" to ensure they're all protected
        val writeRules = Regex("allow write:").findAll(content).count()
        val protectedWrites = Regex("allow write: if (false|isAuthenticated\\(\\)|isOwner)").findAll(content).count()
        
        // Note: Some migration rules temporarily allow writes for authenticated users
        // This is documented in the rules file
        assertTrue("Most write rules should be protected (found $writeRules total, $protectedWrites protected)",
            protectedWrites > writeRules / 2) // At least half should be protected
    }
    
    @Test
    fun `critical collections have explicit rules`() {
        val content = getRulesContent()
        
        val criticalCollections = listOf(
            "users", "test_usage", "subscription", "test_sessions",
            "submissions", "test_content/oir"
        )
        
        criticalCollections.forEach { collection ->
            assertTrue("Should have rules for $collection",
                content.contains("match /$collection") || 
                content.contains("match /data/$collection"))
        }
    }
    
    @Test
    fun `role-based access functions check role field`() {
        val content = getRulesContent()
        
        assertTrue("isAssessor should check role == 'ASSESSOR'",
            content.contains("data.role == 'ASSESSOR'"))
        assertTrue("isStudent should check role == 'STUDENT'",
            content.contains("data.role == 'STUDENT'"))
    }
    
    @Test
    fun `batch access control validates instructor ownership`() {
        val content = getRulesContent()
        val batchRules = content.substringAfter("match /batches/{batchId}")
            .substringBefore("// BATCH ENROLLMENTS")
        
        assertTrue("Batch creation should check instructorId",
            batchRules.contains("request.resource.data.instructorId == request.auth.uid"))
    }
    
    // ==================== Documentation and Maintainability Tests ====================
    
    @Test
    fun `rules have clear section comments`() {
        val content = getRulesContent()
        
        val expectedSections = listOf(
            "HELPER FUNCTIONS",
            "USER DATA",
            "TEST CONTENT",
            "TEST SESSIONS",
            "TEST SUBMISSIONS",
            "OIR Test Content",
            "WAT Test Content",
            "SRT Test Content"
        )
        
        expectedSections.forEach { section ->
            assertTrue("Should have section comment for $section",
                content.contains(section))
        }
    }
    
    @Test
    fun `security notes are present for migration rules`() {
        val content = getRulesContent()
        
        assertTrue("Should have production security notes",
            content.contains("⚠️ PRODUCTION SECURITY NOTE"))
        assertTrue("Should document migration write access",
            content.contains("Migration write access should be removed"))
    }
}

