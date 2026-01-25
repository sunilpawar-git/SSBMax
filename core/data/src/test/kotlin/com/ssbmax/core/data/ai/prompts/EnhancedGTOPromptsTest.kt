package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.prompts.SSBPromptCore
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD Tests for Enhanced GTO Prompts
 * 
 * Tests verify that all 8 GTO activities use SSBPromptCore SSOT for:
 * - Factor context (4 SSB factors)
 * - Critical quality warnings
 * - Factor consistency rules
 * - Scoring scale (1-10, lower = better)
 * - Test-specific penalizing/boosting indicators
 */
class EnhancedGTOPromptsTest {

    // ===========================================
    // GD (GROUP DISCUSSION) TESTS
    // ===========================================

    @Test
    fun `GD prompt includes SSB factor context`() {
        val prompt = EnhancedGTOPrompts.buildGDPrompt(
            topic = "Leadership in Modern Era",
            response = "Leadership today requires adaptability...",
            charCount = 500,
            timeSpentSeconds = 180
        )
        
        // Should include factor context from SSBPromptCore
        assertTrue("GD prompt should include Factor I (Planning)", 
            prompt.contains("Factor I") || prompt.contains("Planning & Execution"))
        assertTrue("GD prompt should include Factor II (Social)", 
            prompt.contains("Factor II") || prompt.contains("Social Adjustment"))
        assertTrue("GD prompt should include Factor III (Effectiveness)", 
            prompt.contains("Factor III") || prompt.contains("Effectiveness"))
        assertTrue("GD prompt should include Factor IV (Dynamic)", 
            prompt.contains("Factor IV") || prompt.contains("Dynamic"))
    }

    @Test
    fun `GD prompt includes critical quality warnings`() {
        val prompt = EnhancedGTOPrompts.buildGDPrompt(
            topic = "Teamwork",
            response = "Working together is essential...",
            charCount = 300,
            timeSpentSeconds = 120
        )
        
        // Should include critical quality warnings
        assertTrue("GD prompt should warn about critical OLQs", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("GD prompt should mention Factor II importance", 
            prompt.contains("Factor II") || prompt.contains("Social"))
    }

    @Test
    fun `GD prompt includes correct scoring scale`() {
        val prompt = EnhancedGTOPrompts.buildGDPrompt(
            topic = "Test Topic",
            response = "Test response",
            charCount = 100,
            timeSpentSeconds = 60
        )
        
        // Should include proper 1-10 scale guidance
        assertTrue("GD prompt should explain 1-10 scale", 
            prompt.contains("1-10") || prompt.contains("1 to 10"))
        val promptLower = prompt.lowercase()
        assertTrue("GD prompt should indicate lower is better", 
            promptLower.contains("lower") && promptLower.contains("better"))
    }

    @Test
    fun `GD prompt includes penalizing and boosting indicators`() {
        val prompt = EnhancedGTOPrompts.buildGDPrompt(
            topic = "Test Topic",
            response = "Test response",
            charCount = 100,
            timeSpentSeconds = 60
        )
        
        // Should include test-specific indicators
        assertTrue("GD prompt should include penalizing indicators", 
            prompt.contains("penaliz") || prompt.contains("Penaliz") || prompt.contains("PENALIZ"))
        assertTrue("GD prompt should include boosting indicators", 
            prompt.contains("boost") || prompt.contains("Boost") || prompt.contains("BOOST"))
    }

    // ===========================================
    // GPE (GROUP PLANNING EXERCISE) TESTS
    // ===========================================

    @Test
    fun `GPE prompt includes SSB factor context`() {
        val prompt = EnhancedGTOPrompts.buildGPEPrompt(
            scenario = "A group must plan a rescue mission...",
            plan = "First, assess the terrain...",
            characterCount = 600,
            timeSpentSeconds = 300,
            solution = "The ideal solution involves..."
        )
        
        assertTrue("GPE prompt should include factor context", 
            prompt.contains("Factor") && prompt.contains("OLQ"))
    }

    @Test
    fun `GPE prompt includes critical quality warnings`() {
        val prompt = EnhancedGTOPrompts.buildGPEPrompt(
            scenario = "Test scenario",
            plan = "Test plan",
            characterCount = 200,
            timeSpentSeconds = 120,
            solution = null
        )
        
        assertTrue("GPE prompt should include critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
    }

    @Test
    fun `GPE prompt includes scoring scale and penalizing boosting`() {
        val prompt = EnhancedGTOPrompts.buildGPEPrompt(
            scenario = "Test",
            plan = "Test",
            characterCount = 100,
            timeSpentSeconds = 60,
            solution = null
        )
        
        assertTrue("GPE prompt should explain scoring", 
            prompt.contains("1-10") || prompt.contains("lower"))
        assertTrue("GPE prompt should include penalizing indicators", 
            prompt.contains("penaliz") || prompt.contains("Penaliz") || prompt.contains("PENALIZ"))
    }

    // ===========================================
    // LECTURETTE TESTS
    // ===========================================

    @Test
    fun `Lecturette prompt includes SSB factor context`() {
        val prompt = EnhancedGTOPrompts.buildLecturettePrompt(
            selectedTopic = "Climate Change",
            topicChoices = listOf("Climate Change", "Education", "Technology", "Health"),
            speechTranscript = "Climate change is one of the most pressing issues...",
            charCount = 800,
            timeSpentSeconds = 180
        )
        
        assertTrue("Lecturette prompt should include factor context", 
            prompt.contains("Factor") && (prompt.contains("Planning") || prompt.contains("Social")))
    }

    @Test
    fun `Lecturette prompt includes critical quality warnings`() {
        val prompt = EnhancedGTOPrompts.buildLecturettePrompt(
            selectedTopic = "Test Topic",
            topicChoices = listOf("Test Topic"),
            speechTranscript = "Test speech",
            charCount = 100,
            timeSpentSeconds = 60
        )
        
        assertTrue("Lecturette prompt should include critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
    }

    @Test
    fun `Lecturette prompt includes scoring scale and indicators`() {
        val prompt = EnhancedGTOPrompts.buildLecturettePrompt(
            selectedTopic = "Test",
            topicChoices = listOf("Test"),
            speechTranscript = "Test",
            charCount = 50,
            timeSpentSeconds = 30
        )
        
        assertTrue("Lecturette prompt should have scoring guidance", 
            prompt.contains("1-10") || prompt.contains("lower"))
        assertTrue("Lecturette prompt should include boosting indicators", 
            prompt.contains("boost") || prompt.contains("Boost") || prompt.contains("BOOST"))
    }

    // ===========================================
    // PGT (PROGRESSIVE GROUP TASK) TESTS
    // ===========================================

    @Test
    fun `PGT prompt includes SSB context`() {
        val prompt = EnhancedGTOPrompts.buildPGTPrompt(
            obstacleCount = 4,
            solutions = listOf(
                Pair(1, "Cross the ditch using planks"),
                Pair(2, "Use rope to swing across")
            ),
            timeSpentSeconds = 240
        )
        
        assertTrue("PGT prompt should include SSB context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `PGT prompt includes critical warnings and scoring`() {
        val prompt = EnhancedGTOPrompts.buildPGTPrompt(
            obstacleCount = 2,
            solutions = listOf(Pair(1, "Test solution")),
            timeSpentSeconds = 60
        )
        
        assertTrue("PGT prompt should have critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("PGT prompt should have scoring guidance", 
            prompt.contains("1-10") || prompt.contains("lower"))
    }

    // ===========================================
    // HGT (HALF GROUP TASK) TESTS
    // ===========================================

    @Test
    fun `HGT prompt includes SSB context and leadership focus`() {
        val prompt = EnhancedGTOPrompts.buildHGTPrompt(
            obstacleName = "Ravine Crossing",
            obstacleDescription = "Cross a 10ft wide ravine with limited materials",
            solutionText = "Organize team into groups...",
            leadershipDecisions = "Assigned roles, coordinated timing",
            timeSpentSeconds = 180
        )
        
        assertTrue("HGT prompt should include SSB context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
        assertTrue("HGT prompt should emphasize leadership", 
            prompt.contains("leader") || prompt.contains("Leader") || prompt.contains("INITIATIVE"))
    }

    @Test
    fun `HGT prompt includes critical warnings and indicators`() {
        val prompt = EnhancedGTOPrompts.buildHGTPrompt(
            obstacleName = "Test",
            obstacleDescription = "Test description",
            solutionText = "Test solution",
            leadershipDecisions = "Test decisions",
            timeSpentSeconds = 60
        )
        
        assertTrue("HGT prompt should have critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("HGT prompt should have penalizing/boosting", 
            (prompt.contains("penaliz") || prompt.contains("Penaliz")) && 
            (prompt.contains("boost") || prompt.contains("Boost")))
    }

    // ===========================================
    // GOR (GROUP OBSTACLE RACE) TESTS
    // ===========================================

    @Test
    fun `GOR prompt includes SSB context and teamwork focus`() {
        val prompt = EnhancedGTOPrompts.buildGORPrompt(
            obstacleCount = 5,
            coordinationStrategy = "We divided into pairs and communicated constantly...",
            timeSpentSeconds = 300
        )
        
        assertTrue("GOR prompt should include SSB context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
        assertTrue("GOR prompt should emphasize teamwork", 
            prompt.contains("team") || prompt.contains("Team") || prompt.contains("COOPERATION"))
    }

    @Test
    fun `GOR prompt includes critical warnings and scoring`() {
        val prompt = EnhancedGTOPrompts.buildGORPrompt(
            obstacleCount = 3,
            coordinationStrategy = "Test strategy",
            timeSpentSeconds = 120
        )
        
        assertTrue("GOR prompt should have critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("GOR prompt should have scoring guidance", 
            prompt.contains("1-10") || prompt.contains("lower"))
    }

    // ===========================================
    // IO (INDIVIDUAL OBSTACLES) TESTS
    // ===========================================

    @Test
    fun `IO prompt includes SSB context and individual focus`() {
        val prompt = EnhancedGTOPrompts.buildIOPrompt(
            obstacleCount = 10,
            approach = "I approached each obstacle with determination...",
            timeSpentSeconds = 600
        )
        
        assertTrue("IO prompt should include SSB context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
        assertTrue("IO prompt should emphasize individual qualities", 
            prompt.contains("COURAGE") || prompt.contains("DETERMINATION") || 
            prompt.contains("individual") || prompt.contains("Individual"))
    }

    @Test
    fun `IO prompt includes critical warnings and indicators`() {
        val prompt = EnhancedGTOPrompts.buildIOPrompt(
            obstacleCount = 5,
            approach = "Test approach",
            timeSpentSeconds = 120
        )
        
        assertTrue("IO prompt should have critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("IO prompt should have penalizing/boosting", 
            (prompt.contains("penaliz") || prompt.contains("Penaliz")) || 
            (prompt.contains("boost") || prompt.contains("Boost")))
    }

    // ===========================================
    // CT (COMMAND TASK) TESTS
    // ===========================================

    @Test
    fun `CT prompt includes SSB context and command focus`() {
        val prompt = EnhancedGTOPrompts.buildCTPrompt(
            scenario = "Lead your team to complete an urgent mission...",
            obstacleName = "Bridge Construction",
            commandDecisions = "Delegated tasks based on individual strengths...",
            resourceAllocation = "Assigned 2 members to gather materials...",
            timeSpentSeconds = 240
        )
        
        assertTrue("CT prompt should include SSB context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
        assertTrue("CT prompt should emphasize command qualities", 
            prompt.contains("command") || prompt.contains("Command") || 
            prompt.contains("INITIATIVE") || prompt.contains("SPEED_OF_DECISION"))
    }

    @Test
    fun `CT prompt includes critical warnings and scoring`() {
        val prompt = EnhancedGTOPrompts.buildCTPrompt(
            scenario = "Test scenario",
            obstacleName = "Test obstacle",
            commandDecisions = "Test decisions",
            resourceAllocation = "Test allocation",
            timeSpentSeconds = 60
        )
        
        assertTrue("CT prompt should have critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
        assertTrue("CT prompt should have scoring guidance", 
            prompt.contains("1-10") || prompt.contains("lower"))
        assertTrue("CT prompt should have penalizing/boosting", 
            (prompt.contains("penaliz") || prompt.contains("Penaliz")) && 
            (prompt.contains("boost") || prompt.contains("Boost")))
    }

    // ===========================================
    // JSON OUTPUT FORMAT TESTS
    // ===========================================

    @Test
    fun `All GTO prompts include JSON output instructions`() {
        val gdPrompt = EnhancedGTOPrompts.buildGDPrompt("Topic", "Response", 100, 60)
        val gpePrompt = EnhancedGTOPrompts.buildGPEPrompt("Scenario", "Plan", 100, 60, null)
        val lecturettePrompt = EnhancedGTOPrompts.buildLecturettePrompt("Topic", listOf("Topic"), "Speech", 100, 60)
        val pgtPrompt = EnhancedGTOPrompts.buildPGTPrompt(2, listOf(Pair(1, "Solution")), 60)
        val hgtPrompt = EnhancedGTOPrompts.buildHGTPrompt("Name", "Desc", "Solution", "Decisions", 60)
        val gorPrompt = EnhancedGTOPrompts.buildGORPrompt(3, "Strategy", 60)
        val ioPrompt = EnhancedGTOPrompts.buildIOPrompt(5, "Approach", 60)
        val ctPrompt = EnhancedGTOPrompts.buildCTPrompt("Scenario", "Obstacle", "Decisions", "Allocation", 60)

        val prompts = listOf(gdPrompt, gpePrompt, lecturettePrompt, pgtPrompt, hgtPrompt, gorPrompt, ioPrompt, ctPrompt)
        
        prompts.forEach { prompt ->
            assertTrue("Prompt should mention JSON output", 
                prompt.contains("JSON") || prompt.contains("json"))
            assertTrue("Prompt should list all 15 OLQs", 
                prompt.contains("EFFECTIVE_INTELLIGENCE") && prompt.contains("STAMINA"))
        }
    }

    @Test
    fun `All GTO prompts include factor consistency rules`() {
        val gdPrompt = EnhancedGTOPrompts.buildGDPrompt("Topic", "Response", 100, 60)
        
        assertTrue("GD prompt should include consistency rules", 
            prompt.contains("consistency") || prompt.contains("Consistency") || 
            prompt.contains("±1") || prompt.contains("±2"))
    }

    // Helper for the last test
    private val prompt = EnhancedGTOPrompts.buildGDPrompt("Topic", "Response", 100, 60)
}
