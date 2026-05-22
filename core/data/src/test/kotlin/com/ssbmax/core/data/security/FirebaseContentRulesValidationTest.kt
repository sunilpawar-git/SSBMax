package com.ssbmax.core.data.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for Firebase Security Rules validation (Content & Submissions)
 * Part of the rules validation test decomposition to satisfy 300-line limits.
 */
class FirebaseContentRulesValidationTest {
    
    companion object {
        private const val RULES_FILE_PATH = "firestore.rules"
    }
    
    private fun getRulesContent(): String {
        val possiblePaths = listOf(
            File(System.getProperty("user.dir"), RULES_FILE_PATH),
            File(System.getProperty("user.dir"), "../../$RULES_FILE_PATH"),
            File(System.getProperty("user.dir"), "../../../$RULES_FILE_PATH")
        )
        
        val rulesFile = possiblePaths.firstOrNull { it.exists() }
            ?: error("Could not find firestore.rules file.")
        
        return rulesFile.readText()
    }
    
    // ==================== OIR Test Content Tests ====================
    
    @Test
    fun `OIR metadata is read-only for authenticated users`() {
        val content = getRulesContent()
        val oirMetaRules = content.substringAfter("match /test_content/oir/meta/{document}")
            .substringBefore("}")
        
        assertTrue("Authenticated users can read OIR metadata",
            oirMetaRules.contains("allow read: if isAuthenticated()"))
        assertTrue("Clients cannot write OIR metadata",
            oirMetaRules.contains("allow write: if false"))
    }
    
    @Test
    fun `OIR question batches are read-only for authenticated users`() {
        val content = getRulesContent()
        val oirBatchRules = content.substringAfter("match /test_content/oir/question_batches/{batchId}")
            .substringBefore("}")
        
        assertTrue("Authenticated users can read question batches",
            oirBatchRules.contains("allow read: if isAuthenticated()"))
        assertTrue("Clients cannot write question batches",
            oirBatchRules.contains("allow write: if false"))
    }
    
    // ==================== Test Sessions Tests ====================
    
    @Test
    fun `test sessions are user-specific`() {
        val content = getRulesContent()
        val sessionRules = content.substringAfter("match /test_sessions/{sessionId}")
            .substringBefore("// TEST SUBMISSIONS")
        
        assertTrue("Users can read own sessions",
            sessionRules.contains("resource.data.userId == request.auth.uid"))
        assertTrue("Sessions must have userId",
            sessionRules.contains("request.resource.data.userId == request.auth.uid"))
    }
    
    @Test
    fun `test sessions require isActive flag on creation`() {
        val content = getRulesContent()
        val sessionCreate = content.substringAfter("// Users can create test sessions")
            .substringBefore("// Users can update their own sessions")
        
        assertTrue("Sessions must be created with isActive = true",
            sessionCreate.contains("request.resource.data.isActive == true"))
    }
    
    // ==================== Submissions Tests ====================
    
    @Test
    fun `submissions require testType and submittedAt fields`() {
        val content = getRulesContent()
        val submissionCreate = content.substringAfter("// Students can create submissions")
            .substringBefore("// Students can update their own IN_PROGRESS submissions")
        
        assertTrue("Submissions must have testType",
            submissionCreate.contains("'testType'"))
        assertTrue("Submissions must have submittedAt",
            submissionCreate.contains("'submittedAt'"))
    }
    
    @Test
    fun `students can only update IN_PROGRESS submissions`() {
        val content = getRulesContent()
        val submissionUpdate = content.substringAfter("// Students can update their own IN_PROGRESS submissions")
            .substringBefore("// Students can delete their own DRAFT submissions")
        
        assertTrue("Should check IN_PROGRESS status",
            submissionUpdate.contains("resource.data.status == 'IN_PROGRESS'"))
    }
    
    @Test
    fun `students can only delete DRAFT submissions`() {
        val content = getRulesContent()
        val submissionDelete = content.substringAfter("// Students can delete their own DRAFT submissions only")
            .substringBefore("// AI GRADING RESULTS")
        
        assertTrue("Should check DRAFT status",
            submissionDelete.contains("resource.data.status == 'DRAFT'"))
    }
    
    // ==================== AI Grading Results Tests ====================
    
    @Test
    fun `AI grading results are read-only from client`() {
        val content = getRulesContent()
        val aiGradingRules = content.substringAfter("match /ai_grading_results/{resultId}")
            .substringBefore("// NOTIFICATIONS")
        
        assertTrue("Users can read own AI results",
            aiGradingRules.contains("allow read: if isAuthenticated()"))
        assertTrue("Clients cannot write AI results",
            aiGradingRules.contains("allow write: if false"))
    }
    
    // ==================== Study Materials Tests ====================
    
    @Test
    fun `study materials are read-only for authenticated users`() {
        val content = getRulesContent()
        val studyMaterialsRules = content.substringAfter("match /studyMaterials/{materialId}")
            .substringBefore("// USER PROGRESS")
        
        assertTrue("Authenticated users can read study materials",
            studyMaterialsRules.contains("allow read: if isAuthenticated()"))
        assertTrue("Clients cannot write study materials",
            studyMaterialsRules.contains("allow write: if false"))
    }
}
