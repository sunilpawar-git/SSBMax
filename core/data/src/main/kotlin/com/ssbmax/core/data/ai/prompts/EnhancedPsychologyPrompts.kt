package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.prompts.SSBPromptCore

/**
 * Enhanced Psychology Test Prompts using SSBPromptCore as SSOT.
 * 
 * This object provides prompt builders that:
 * 1. Use SSBPromptCore for standardized SSB context
 * 2. Add test-specific penalizing/boosting indicators
 * 3. Maintain JSON output format requirements
 * 4. Include factor consistency and critical quality warnings
 * 
 * All prompts follow the unified SSB scoring framework (1-10, lower = better).
 */
object EnhancedPsychologyPrompts {

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

2. **FACTOR CONSISTENCY**: Ensure scores within each factor are consistent (±1 for strict factors, ±2 for lenient)

3. **CRITICAL OLQ CHECK**: Pay special attention to the 6 critical OLQs - document clear evidence
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
    // TAT (THEMATIC APPERCEPTION TEST)
    // ===========================================

    /**
     * Build enhanced TAT analysis prompt with SSB context.
     */
    fun buildTATPrompt(stories: List<String>): String {
        val storiesText = stories.mapIndexed { index, story ->
            "Story ${index + 1}: $story"
        }.joinToString("\n\n")

        return """
You are analyzing TAT (Thematic Apperception Test) stories for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
TAT STORIES (${stories.size} stories):
═══════════════════════════════════════════════════════════════════════════════

$storiesText

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.TAT)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.TAT)}

═══════════════════════════════════════════════════════════════════════════════
TAT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Look for these elements across all stories:
- **Hero Identification**: Is there a clear protagonist? What actions do they take?
- **Story Structure**: Past-Present-Future flow indicates organizing ability
- **Outcome Quality**: Positive through effort (good) vs. passive luck (poor)
- **Themes**: Leadership, teamwork, responsibility, initiative, courage
- **Response to Obstacles**: Overcoming vs. giving up

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // WAT (WORD ASSOCIATION TEST)
    // ===========================================

    /**
     * Build enhanced WAT analysis prompt with SSB context.
     */
    fun buildWATPrompt(
        responses: List<Pair<String, String>>,
        averageResponseTime: Double
    ): String {
        val responsesText = responses.mapIndexed { index, (word, response) ->
            "${index + 1}. $word → $response"
        }.joinToString("\n")

        return """
You are analyzing WAT (Word Association Test) responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
WAT RESPONSES (${responses.size} word associations):
═══════════════════════════════════════════════════════════════════════════════

$responsesText

Average response time: ${String.format("%.1f", averageResponseTime)}s

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.WAT)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.WAT)}

═══════════════════════════════════════════════════════════════════════════════
WAT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Analyze word association patterns for:
- **Response Quality**: Creative vs. repetitive, positive vs. negative
- **Consistency**: Similar themes indicate stable personality traits
- **Response Time**: Fast responses show spontaneity and confidence
- **Association Type**: Action-oriented, social, goal-focused patterns
- **Vocabulary Range**: Indicates intellectual breadth

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // SRT (SITUATION REACTION TEST)
    // ===========================================

    /**
     * Build enhanced SRT analysis prompt with SSB context.
     */
    fun buildSRTPrompt(situations: List<Pair<String, String>>): String {
        val situationsText = situations.mapIndexed { index, (situation, response) ->
            "${index + 1}. Situation: $situation\n   Response: $response"
        }.joinToString("\n\n")

        return """
You are analyzing SRT (Situation Reaction Test) responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
SRT RESPONSES (${situations.size} situations):
═══════════════════════════════════════════════════════════════════════════════

$situationsText

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getLimitationGuidance()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.SRT)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.SRT)}

═══════════════════════════════════════════════════════════════════════════════
SRT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Evaluate each situation response for:
- **Action Orientation**: Immediate practical action vs. avoidance
- **Responsibility**: Taking ownership vs. blaming others
- **Problem-Solving**: Logical approach vs. emotional reaction
- **Ethics**: Honest, principled responses vs. shortcuts
- **Follow-Through**: Complete solutions vs. partial attempts

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // SDT (SELF DESCRIPTION TEST)
    // ===========================================

    /**
     * Build enhanced SDT analysis prompt with SSB context.
     */
    fun buildSDTPrompt(descriptions: Map<String, String>): String {
        val descriptionsText = descriptions.entries.joinToString("\n\n") { (perspective, content) ->
            val label = when (perspective.lowercase()) {
                "parents" -> "Parents' Opinion"
                "teachers" -> "Teachers/Seniors' Opinion"
                "friends" -> "Friends' Opinion"
                "self" -> "Own Opinion (Self-View)"
                else -> perspective
            }
            "$label:\n$content"
        }

        return """
You are analyzing SDT (Self Description Test) responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
SELF DESCRIPTION RESPONSES:
═══════════════════════════════════════════════════════════════════════════════

$descriptionsText

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.SDT)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.SDT)}

═══════════════════════════════════════════════════════════════════════════════
SDT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Analyze self-description for:
- **Self-Awareness**: Accurate understanding of strengths/weaknesses
- **Consistency**: Alignment between different perspectives
- **Discrepancy Analysis**: Major gaps indicate poor self-awareness
- **Growth Orientation**: Acknowledging areas for improvement
- **Realism**: Neither overly modest nor grandiose

Key discrepancy check:
- If self-view is significantly more positive than others' views → Lower SELF_CONFIDENCE (overconfidence)
- If self-view is significantly more negative than others' views → Lower SELF_CONFIDENCE (under-confidence)

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }

    // ===========================================
    // PPDT (PICTURE PERCEPTION & DESCRIPTION TEST)
    // ===========================================

    /**
     * Build enhanced PPDT analysis prompt with SSB context.
     */
    fun buildPPDTPrompt(
        story: String,
        charactersCount: Int,
        writingTimeMinutes: Double,
        imageContext: String,
        candidateGender: String
    ): String {
        val genderGuidance = when (candidateGender.lowercase()) {
            "male" -> "The candidate is male - a male protagonist in the story is typically preferred but not required."
            "female" -> "The candidate is female - a female protagonist in the story is typically preferred but not required."
            else -> "Protagonist gender is not constrained."
        }

        return """
You are analyzing PPDT (Picture Perception & Description Test) story for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
IMAGE CONTEXT:
═══════════════════════════════════════════════════════════════════════════════

$imageContext

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE'S STORY:
═══════════════════════════════════════════════════════════════════════════════

$story

(Characters identified: $charactersCount, Writing time: ${String.format("%.1f", writingTimeMinutes)} minutes)

$genderGuidance

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.PPDT)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.PPDT)}

═══════════════════════════════════════════════════════════════════════════════
PPDT-SPECIFIC ASSESSMENT GUIDANCE:
═══════════════════════════════════════════════════════════════════════════════

Evaluate the story for:
- **Hero Identification**: Clear protagonist who drives action
- **Picture Perception**: Accurate interpretation of the image
- **Story Structure**: Clear Past-Present-Future format boosts ORGANIZING_ABILITY
- **Goal Setting**: If goal is established in first 2-3 lines, boost SPEED_OF_DECISION
- **Outcome**: Positive outcome through hero's effort (not luck or external help)
- **Avoid**: Material rewards (money, prize, trophy) cap maximum scores

Key scoring hints:
- Proactive hero who takes initiative → Boost INITIATIVE, COURAGE
- Teamwork and helping others → Boost COOPERATION, SOCIAL_ADJUSTMENT
- Clear problem-solving approach → Boost REASONING_ABILITY, EFFECTIVE_INTELLIGENCE
- Optimistic ending through effort → Boost DETERMINATION, LIVELINESS

${getValidationInstructions()}

${getJsonOutputInstructions()}

${getJsonOutputFormat()}
""".trimIndent()
    }
}
