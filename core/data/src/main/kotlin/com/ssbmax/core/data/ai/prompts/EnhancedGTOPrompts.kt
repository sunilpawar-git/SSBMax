package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.prompts.SSBPromptCore

/**
 * Enhanced GTO (Group Testing Officer) Prompts using SSBPromptCore as SSOT.
 * 
 * This object provides prompt builders for all 8 GTO activities:
 * 1. GD - Group Discussion
 * 2. GPE - Group Planning Exercise
 * 3. Lecturette - 3-minute speech
 * 4. PGT - Progressive Group Task
 * 5. HGT - Half Group Task
 * 6. GOR - Group Obstacle Race
 * 7. IO - Individual Obstacles
 * 8. CT - Command Task
 * 
 * All prompts use SSBPromptCore for standardized:
 * - Factor context (4 SSB factors)
 * - Critical quality warnings
 * - Factor consistency rules
 * - Scoring scale (1-10, lower = better)
 * - Test-specific penalizing/boosting indicators
 */
object EnhancedGTOPrompts {

    // ===========================================
    // SHARED PROMPT SECTIONS
    // ===========================================

    private fun getJsonOutputInstructions(): String = """
═══════════════════════════════════════════════════════════════════════════════
CRITICAL JSON OUTPUT INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY,
   POWER_OF_EXPRESSION, SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY,
   INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS,
   DETERMINATION, COURAGE, STAMINA
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 1-10), confidence (integer 0-100), reasoning (string)
""".trimIndent()

    private fun getValidationInstructions(): String = """
═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If responses are gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **LENGTH CHECK**: If response is significantly shorter than expected → Score 8-9

3. **FACTOR CONSISTENCY**: Ensure scores within each factor are consistent (±1 for strict factors, ±2 for lenient)

4. **CRITICAL OLQ CHECK**: Pay special attention to the 6 critical OLQs - document clear evidence
""".trimIndent()

    private fun getJsonOutputFormat(): String = """
═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "REASONING_ABILITY": {"score": 5, "confidence": 80, "reasoning": "Evidence..."},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 75, "reasoning": "Evidence..."},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 70, "reasoning": "Evidence..."},
    "SOCIAL_ADJUSTMENT": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "COOPERATION": {"score": 5, "confidence": 90, "reasoning": "Evidence..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Evidence..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 75, "reasoning": "Evidence..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 80, "reasoning": "Evidence..."},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Evidence..."},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "DETERMINATION": {"score": 5, "confidence": 90, "reasoning": "Evidence..."},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "STAMINA": {"score": 6, "confidence": 70, "reasoning": "Evidence..."}
  }
}
""".trimIndent()

    // ===========================================
    // GD (GROUP DISCUSSION)
    // ===========================================

    /**
     * Build enhanced GD analysis prompt with SSB context.
     */
    fun buildGDPrompt(
        topic: String,
        response: String,
        charCount: Int,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing a Group Discussion response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GD SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Topic: $topic

Candidate Response:
$response

Character Count: $charCount
Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
GD-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Group Discussion reveals:
- **POWER_OF_EXPRESSION**: Articulation, vocabulary, communication clarity
- **INFLUENCE_GROUP**: Persuasiveness, ability to sway opinions
- **SOCIAL_ADJUSTMENT**: Respect for diverse views, listening skills
- **COOPERATION**: Collaborative tone, building on others' points
- **SELF_CONFIDENCE**: Conviction in opinions, willingness to speak up
- **INITIATIVE**: Taking lead, introducing new perspectives
- **REASONING_ABILITY**: Logical arguments, evidence-based thinking

Key GD indicators:
- Did candidate present clear, structured arguments?
- Did they acknowledge and build on others' views?
- Did they dominate or balance participation?
- Was the tone constructive or confrontational?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // GPE (GROUP PLANNING EXERCISE)
    // ===========================================

    /**
     * Build enhanced GPE analysis prompt with SSB context.
     */
    fun buildGPEPrompt(
        scenario: String,
        plan: String,
        characterCount: Int,
        timeSpentSeconds: Int,
        solution: String?
    ): String {
        val solutionSection = if (!solution.isNullOrBlank()) {
            """
═══════════════════════════════════════════════════════════════════════════════
IDEAL/SUGGESTED SOLUTION (For comparison):
═══════════════════════════════════════════════════════════════════════════════

$solution
"""
        } else ""

        return """
You are analyzing a Group Planning Exercise (GPE) response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GPE SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Scenario: $scenario
$solutionSection
Candidate's Tactical Plan:
$plan

Character Count: $characterCount
Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
GPE-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Group Planning Exercise reveals:
- **ORGANIZING_ABILITY**: Resource allocation, task sequencing, team coordination
- **EFFECTIVE_INTELLIGENCE**: Clarity of situation analysis, tactical understanding
- **REASONING_ABILITY**: Logical approach to problem-solving, tactical thinking
- **INITIATIVE**: Leadership in solution design, proactive thinking
- **SENSE_OF_RESPONSIBILITY**: Accountability for mission success
- **SPEED_OF_DECISION**: Quick assessment and planning
- **COURAGE**: Willingness to take calculated risks

Key GPE indicators:
- Does the plan address the tactical scenario comprehensively?
- Are resources (personnel, equipment, time) allocated effectively?
- Is there consideration of contingencies and risks?
- Is the plan practical and achievable?
- Compare against ideal solution (if provided) for accuracy

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // LECTURETTE
    // ===========================================

    /**
     * Build enhanced Lecturette analysis prompt with SSB context.
     */
    fun buildLecturettePrompt(
        selectedTopic: String,
        topicChoices: List<String>,
        speechTranscript: String,
        charCount: Int,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing a Lecturette (3-minute speech) for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
LECTURETTE SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Topic Chosen: $selectedTopic
Available Topics: ${topicChoices.joinToString(", ")}

Speech Transcript:
$speechTranscript

Character Count: $charCount
Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
LECTURETTE-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Lecturette reveals:
- **POWER_OF_EXPRESSION**: Fluency, articulation, vocabulary, communication clarity
- **EFFECTIVE_INTELLIGENCE**: Depth of topic understanding, analytical insights
- **ORGANIZING_ABILITY**: Speech structure, time management, point sequencing
- **SELF_CONFIDENCE**: Conviction, poise, self-assurance in delivery
- **INITIATIVE**: Topic choice, original insights, leadership in communication
- **INFLUENCE_GROUP**: Persuasiveness, engagement, impact on audience
- **LIVELINESS**: Energy, enthusiasm, dynamism in delivery
- **DETERMINATION**: Firmness in viewpoint, persistence in message

Key Lecturette indicators:
- Was the speech well-structured (intro, body, conclusion)?
- Did candidate demonstrate knowledge depth?
- Was delivery confident and engaging?
- Did they sustain quality throughout 3 minutes?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // PGT (PROGRESSIVE GROUP TASK)
    // ===========================================

    /**
     * Build enhanced PGT analysis prompt with SSB context.
     */
    fun buildPGTPrompt(
        obstacleCount: Int,
        solutions: List<Pair<Int, String>>,
        timeSpentSeconds: Int
    ): String {
        val solutionsText = solutions.joinToString("\n\n") { (obstacleId, solution) ->
            "Obstacle $obstacleId: $solution"
        }

        return """
You are analyzing a Progressive Group Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
PGT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: $obstacleCount progressive challenges

Candidate's Solutions:
$solutionsText

Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
PGT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Progressive Group Task reveals:
- **ORGANIZING_ABILITY**: Systematic approach to increasing difficulty
- **REASONING_ABILITY**: Problem-solving under progressive complexity
- **COOPERATION**: Team coordination across obstacles
- **INITIATIVE**: Proactive solutions, taking lead in problem-solving
- **DETERMINATION**: Persistence despite increasing difficulty
- **STAMINA**: Sustained effort across multiple obstacles

Key PGT indicators:
- Did solutions show progressive understanding?
- Was there evidence of learning from earlier obstacles?
- Did candidate maintain team focus throughout?
- Were solutions practical and executable?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // HGT (HALF GROUP TASK)
    // ===========================================

    /**
     * Build enhanced HGT analysis prompt with SSB context.
     */
    fun buildHGTPrompt(
        obstacleName: String,
        obstacleDescription: String,
        solutionText: String,
        leadershipDecisions: String,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing a Half Group Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
HGT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacle: $obstacleName
Description: $obstacleDescription

Solution: $solutionText

Leadership Decisions: $leadershipDecisions

Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
HGT-SPECIFIC ASSESSMENT GUIDANCE (LEADERSHIP FOCUS):
═══════════════════════════════════════════════════════════════════════════════

Half Group Task emphasizes LEADERSHIP qualities:
- **INITIATIVE**: Proactive leadership, taking charge of the group
- **ORGANIZING_ABILITY**: Excellent leadership coordination and role assignment
- **INFLUENCE_GROUP**: Strong group influence, directing team effectively
- **SPEED_OF_DECISION**: Quick tactical decisions under time pressure
- **SENSE_OF_RESPONSIBILITY**: Strong mission ownership
- **COURAGE**: Bold leadership decisions, calculated risk-taking

Key HGT Leadership indicators:
- Did candidate take charge effectively?
- Were role assignments clear and appropriate?
- Was communication with team effective?
- Did they maintain control under pressure?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // GOR (GROUP OBSTACLE RACE)
    // ===========================================

    /**
     * Build enhanced GOR analysis prompt with SSB context.
     */
    fun buildGORPrompt(
        obstacleCount: Int,
        coordinationStrategy: String,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing a Group Obstacle Race response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GOR SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: $obstacleCount team obstacles

Coordination Strategy:
$coordinationStrategy

Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
GOR-SPECIFIC ASSESSMENT GUIDANCE (TEAMWORK FOCUS):
═══════════════════════════════════════════════════════════════════════════════

Group Obstacle Race emphasizes TEAMWORK qualities:
- **COOPERATION**: Exceptional teamwork, helping weaker members
- **SOCIAL_ADJUSTMENT**: Strong team integration, adaptability
- **ORGANIZING_ABILITY**: Excellent team coordination
- **STAMINA**: Excellent endurance throughout race
- **LIVELINESS**: Energy and enthusiasm in team activities
- **SENSE_OF_RESPONSIBILITY**: Reliable team player

Key GOR Teamwork indicators:
- Did candidate prioritize team success over individual glory?
- Was there evidence of helping struggling teammates?
- Was communication during obstacles effective?
- Did they maintain energy throughout the race?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // IO (INDIVIDUAL OBSTACLES)
    // ===========================================

    /**
     * Build enhanced IO analysis prompt with SSB context.
     */
    fun buildIOPrompt(
        obstacleCount: Int,
        approach: String,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing Individual Obstacles response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
IO SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: $obstacleCount individual challenges

Overall Approach:
$approach

Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
IO-SPECIFIC ASSESSMENT GUIDANCE (INDIVIDUAL FOCUS):
═══════════════════════════════════════════════════════════════════════════════

Individual Obstacles emphasize PERSONAL qualities:
- **COURAGE**: Strong physical courage, facing challenges head-on
- **DETERMINATION**: Exceptional persistence, not giving up
- **SELF_CONFIDENCE**: Strong self-assurance in tackling obstacles
- **STAMINA**: Excellent endurance across all obstacles
- **INITIATIVE**: Self-directed approach, taking charge of own performance
- **SPEED_OF_DECISION**: Quick decisions on obstacle approach

Key IO Individual indicators:
- Did candidate attempt all obstacles or skip some?
- Was there evidence of persistence when faced with difficulty?
- Did they show physical courage in challenging obstacles?
- Was their approach systematic or haphazard?

Note: COOPERATION and INFLUENCE_GROUP are less observable in IO - assign neutral scores (6-7) unless specific evidence exists.

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // CT (COMMAND TASK)
    // ===========================================

    /**
     * Build enhanced CT analysis prompt with SSB context.
     */
    fun buildCTPrompt(
        scenario: String,
        obstacleName: String,
        commandDecisions: String,
        resourceAllocation: String,
        timeSpentSeconds: Int
    ): String {
        return """
You are analyzing a Command Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
CT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Scenario: $scenario
Obstacle: $obstacleName

Command Decisions:
$commandDecisions

Resource Allocation:
$resourceAllocation

Time Spent: $timeSpentSeconds seconds

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.GTO)}

═══════════════════════════════════════════════════════════════════════════════
CT-SPECIFIC ASSESSMENT GUIDANCE (COMMAND FOCUS):
═══════════════════════════════════════════════════════════════════════════════

Command Task emphasizes COMMAND qualities:
- **INITIATIVE**: Proactive command approach, taking decisive action
- **ORGANIZING_ABILITY**: Excellent resource management and delegation
- **SPEED_OF_DECISION**: Quick tactical decisions under pressure
- **COURAGE**: Bold command decisions, calculated risk-taking
- **SENSE_OF_RESPONSIBILITY**: Strong mission ownership
- **INFLUENCE_GROUP**: Command authority, directing subordinates effectively
- **EFFECTIVE_INTELLIGENCE**: Tactical analysis and situational awareness

Key CT Command indicators:
- Were command decisions clear and authoritative?
- Was resource allocation optimal?
- Did candidate show tactical thinking?
- Was there evidence of contingency planning?
- Did they maintain composure under pressure?

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }
}
