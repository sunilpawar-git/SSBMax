package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.prompts.SSBPromptCore

/**
 * Enhanced Interview Prompts using SSBPromptCore as SSOT.
 * 
 * This object provides prompt builders for SSB interview assessment:
 * 1. Question Generation - Personalized questions based on PIQ
 * 2. Adaptive Follow-up - Probing weak OLQs deeper
 * 3. Response Analysis - Scoring responses with SSB framework
 * 4. Feedback Generation - Comprehensive performance feedback
 * 
 * All prompts use SSBPromptCore for standardized:
 * - Factor context (4 SSB factors)
 * - Critical quality warnings
 * - Factor consistency rules
 * - Scoring scale (1-10, lower = better)
 * - Interview-specific penalizing/boosting indicators
 */
object EnhancedInterviewPrompts {

    // ===========================================
    // OLQ DEFINITIONS (Enhanced with SSB context)
    // ===========================================

    private fun getOLQDefinitions(): String = """
═══════════════════════════════════════════════════════════════════════════════
OFFICER-LIKE QUALITIES (OLQs) - Definitions & Behavioral Indicators:
═══════════════════════════════════════════════════════════════════════════════

FACTOR I - INTELLECTUAL QUALITIES (Planning & Execution):
1. EFFECTIVE_INTELLIGENCE
   Definition: Practical wisdom, ability to grasp situations quickly and take appropriate action
   Indicators: Common sense solutions, quick understanding, practical approach to problems
   Questions to reveal: Problem-solving scenarios, real-life challenges faced

2. REASONING_ABILITY [CRITICAL]
   Definition: Logical thinking, ability to analyze cause-effect relationships
   Indicators: Structured thinking, analytical approach, connecting dots
   Questions to reveal: Why questions, asking to explain decisions, hypothetical dilemmas

3. ORGANIZING_ABILITY
   Definition: Systematic planning, resource management, methodical approach
   Indicators: Planning before action, prioritization, delegation awareness
   Questions to reveal: How they planned events, managed projects, handled multiple tasks

4. POWER_OF_EXPRESSION
   Definition: Clear articulation, effective communication, vocabulary usage
   Indicators: Coherent speech, appropriate vocabulary, confident delivery
   Questions to reveal: Descriptive questions, asking to explain complex topics simply

FACTOR II - SOCIAL QUALITIES (MOST CRITICAL FACTOR):
⚠️ If overall Factor II score = 8, candidate is automatically rejected.

5. SOCIAL_ADJUSTMENT [CRITICAL]
   Definition: Adaptability to different social situations, mixing with diverse people
   Indicators: Comfort in varied settings, respect for others, flexibility
   Questions to reveal: Experiences with different groups, handling cultural differences

6. COOPERATION [CRITICAL]
   Definition: Team player, willingness to help, putting group before self
   Indicators: Collaborative spirit, helping others, accepting others' ideas
   Questions to reveal: Team experiences, times they helped others, conflict resolution

7. SENSE_OF_RESPONSIBILITY [CRITICAL]
   Definition: Accountability, reliability, duty-consciousness
   Indicators: Owning mistakes, completing commitments, dependability
   Questions to reveal: Responsibilities held, times they owned up to mistakes

FACTOR III - DYNAMIC QUALITIES (Social Effectiveness):
8. INITIATIVE
   Definition: Self-starter, proactive action without waiting for instructions
   Indicators: Volunteering, starting things independently, not waiting to be told
   Questions to reveal: Times they started something new, proactive problem-solving

9. SELF_CONFIDENCE
   Definition: Belief in own abilities, composure under pressure
   Indicators: Calm demeanor, positive self-talk, handling criticism well
   Questions to reveal: Challenging situations, how they handled failures

10. SPEED_OF_DECISION
    Definition: Quick decision-making without over-analysis
    Indicators: Timely decisions, comfortable with uncertainty, action-oriented
    Questions to reveal: Time-pressure situations, snap decisions made

11. INFLUENCE_GROUP
    Definition: Natural leadership, ability to convince and motivate others
    Indicators: Others follow their suggestions, can persuade without authority
    Questions to reveal: Leadership experiences, times they changed group opinion

12. LIVELINESS [CRITICAL]
    Definition: Energy, enthusiasm, positive outlook
    Indicators: Active participation, optimistic attitude, energetic demeanor
    Questions to reveal: Hobbies, how they energize others, daily routine

FACTOR IV - CHARACTER & PHYSICAL QUALITIES (Dynamic):
13. DETERMINATION
    Definition: Persistence, goal-oriented, doesn't give up easily
    Indicators: Long-term goal pursuit, overcoming obstacles, bouncing back
    Questions to reveal: Long-term goals, times they persisted despite difficulties

14. COURAGE [CRITICAL]
    Definition: Physical and moral courage, standing up for beliefs
    Indicators: Speaking truth, facing fears, defending principles
    Questions to reveal: Times they stood up for something, faced fears

15. STAMINA
    Definition: Physical and mental endurance, resilience
    Indicators: Sustained effort, handling stress, physical fitness
    Questions to reveal: Endurance challenges, stressful periods handled
""".trimIndent()

    // ===========================================
    // PIQ TO OLQ MAPPING
    // ===========================================

    private fun getPIQToOLQMapping(): String = """
═══════════════════════════════════════════════════════════════════════════════
PIQ SECTIONS → OLQ ASSESSMENT MAPPING:
═══════════════════════════════════════════════════════════════════════════════

PERSONAL BACKGROUND:
- Rural/Small town → SOCIAL_ADJUSTMENT (adaptability), STAMINA (hardships faced)
- Urban/Metro → POWER_OF_EXPRESSION (exposure), INFLUENCE_GROUP (diverse experiences)
- Relocated for studies/work → INITIATIVE, DETERMINATION

FAMILY ENVIRONMENT:
- Defense family → SENSE_OF_RESPONSIBILITY, COURAGE (values absorbed)
- Single parent → DETERMINATION, STAMINA (challenges overcome)
- Business family → INITIATIVE, ORGANIZING_ABILITY
- First-generation graduate → DETERMINATION, SELF_CONFIDENCE

EDUCATION JOURNEY:
- Boarding school → SOCIAL_ADJUSTMENT, COOPERATION
- High academic performance → EFFECTIVE_INTELLIGENCE, DETERMINATION
- Stream change → INITIATIVE, SPEED_OF_DECISION
- Co-curricular achievements → Varies based on activity

CAREER & WORK:
- Work experience → SENSE_OF_RESPONSIBILITY, ORGANIZING_ABILITY
- Entrepreneurship → INITIATIVE, COURAGE, SELF_CONFIDENCE
- Multiple jobs → SOCIAL_ADJUSTMENT or concerns about DETERMINATION

ACTIVITIES & INTERESTS:
- Team sports → COOPERATION, SOCIAL_ADJUSTMENT, INFLUENCE_GROUP
- Individual sports → SELF_CONFIDENCE, DETERMINATION, STAMINA
- Creative hobbies → POWER_OF_EXPRESSION, LIVELINESS
- Adventure activities → COURAGE, INITIATIVE

LEADERSHIP EXPOSURE:
- NCC training → SENSE_OF_RESPONSIBILITY, ORGANIZING_ABILITY, COURAGE
- Student body positions → INFLUENCE_GROUP, INITIATIVE
- Event organization → ORGANIZING_ABILITY, EFFECTIVE_INTELLIGENCE
""".trimIndent()

    // ===========================================
    // SHARED PROMPT SECTIONS
    // ===========================================

    private fun getJsonOutputInstructionsQuestions(): String = """
═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON array):
═══════════════════════════════════════════════════════════════════════════════

[
  {
    "id": "q1",
    "questionText": "Your personalized question here?",
    "targetOLQs": ["OLQ_NAME_1", "OLQ_NAME_2"],
    "reasoning": "Why this question for THIS candidate specifically, what OLQ indicators it reveals",
    "piqTouchpoint": "Which PIQ field/detail this references",
    "difficultyLevel": 3,
    "expectedResponseTime": "2-3 minutes"
  }
]

IMPORTANT: 
- Return ONLY the JSON array, no markdown formatting
- Use exact OLQ names (e.g., EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.)
""".trimIndent()

    private fun getJsonOutputInstructionsAnalysis(): String = """
═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Evidence..."},
    "REASONING_ABILITY": {"score": 5, "confidence": 80, "reasoning": "Evidence..."},
    ... (all 15 OLQs)
  },
  "overallConfidence": 75,
  "keyInsights": ["Notable observations about the candidate"],
  "suggestedFollowUp": "A follow-up question to probe further if needed"
}

IMPORTANT:
- Return ONLY the JSON object, no markdown formatting
- Include all targeted OLQs with scores
- Lower scores mean BETTER performance
- Be specific with evidence - quote from their response
""".trimIndent()

    private fun getDifficultyDescription(difficulty: Int): String = when (difficulty) {
        1 -> "Icebreaker - Warm, easy opening questions to build rapport"
        2 -> "Basic Probing - Simple background exploration, direct questions"
        3 -> "Moderate Challenge - Situational questions, 'what would you do' scenarios"
        4 -> "Deep Probing - Challenging assumptions, value-based dilemmas"
        5 -> "Stress Testing - Rapid fire, contradiction exploration, pressure scenarios"
        else -> "Standard difficulty"
    }

    // ===========================================
    // QUESTION GENERATION PROMPT
    // ===========================================

    /**
     * Build comprehensive question generation prompt with SSB context.
     */
    fun buildQuestionGenerationPrompt(
        piqContext: String,
        count: Int,
        difficulty: Int,
        targetOLQs: List<OLQ>?
    ): String {
        val targetOLQGuidance = if (targetOLQs != null && targetOLQs.isNotEmpty()) {
            "Focus primarily on these OLQs: ${targetOLQs.joinToString(", ") { it.name }}"
        } else {
            "Generate a balanced mix covering all OLQ clusters (Intellectual, Social, Dynamic, Character)"
        }

        val difficultyDesc = getDifficultyDescription(difficulty)

        return """
You are a SENIOR SSB PSYCHOLOGIST with 20+ years of experience at Services Selection Board.
Your expertise: Identifying Officer-Like Qualities through strategic, personalized questioning.

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE'S PERSONAL INFORMATION QUESTIONNAIRE (PIQ):
═══════════════════════════════════════════════════════════════════════════════

$piqContext

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${getOLQDefinitions()}

${getPIQToOLQMapping()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Generate exactly $count PERSONALIZED interview questions for this candidate.

DIFFICULTY LEVEL: $difficulty/5 ($difficultyDesc)

OLQ TARGETING: $targetOLQGuidance

═══════════════════════════════════════════════════════════════════════════════
QUESTION REQUIREMENTS (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. PERSONALIZATION IS CRITICAL:
   ✓ MUST reference specific details from the candidate's PIQ
   ✓ Name their hobby, mention their father's profession, cite their college
   ✓ Reference their stated strengths/weaknesses
   ✗ NO generic questions that could apply to anyone

2. OLQ TARGETING:
   ✓ Each question should target 2-3 specific OLQs
   ✓ Explain in reasoning how the answer would reveal those OLQs
   ✓ Balance across OLQ clusters unless specific targeting requested

3. QUESTION TYPES TO INCLUDE (mix these):
   - Background Exploration: "You mentioned [specific PIQ detail]... tell me more about..."
   - Situational: "Given your experience in [PIQ detail], what would you do if..."
   - Value-Based: "Your [family/background] has [characteristic]... how has that shaped..."
   - Achievement Deep-Dive: "You achieved [specific thing]... walk me through the challenges..."
   - Hypothetical Dilemma: Based on their background, present a relevant dilemma

4. QUESTION QUALITY STANDARDS:
   ✓ Open-ended requiring 2-3 minute responses
   ✓ Create natural follow-up opportunities
   ✓ Reveal character through stories, not just opinions
   ✗ NO yes/no questions
   ✗ NO questions answerable in one sentence
   ✗ NO clichéd questions like "Tell me about yourself"

${getJsonOutputInstructionsQuestions()}
""".trimIndent()
    }

    // ===========================================
    // ADAPTIVE FOLLOW-UP PROMPT
    // ===========================================

    /**
     * Build prompt for adaptive follow-up question generation with SSB context.
     */
    fun buildAdaptiveQuestionPrompt(
        piqContext: String,
        previousQA: List<Pair<String, String>>,
        weakOLQs: List<OLQ>,
        count: Int
    ): String {
        val qaHistory = previousQA.mapIndexed { index, (q, a) ->
            """
Question ${index + 1}: $q
Response: $a
            """.trimIndent()
        }.joinToString("\n\n")

        val weakOLQNames = weakOLQs.joinToString(", ") { it.displayName }
        val weakOLQEnums = weakOLQs.joinToString(", ") { it.name }

        return """
You are a SENIOR SSB PSYCHOLOGIST conducting adaptive follow-up assessment.

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE PIQ CONTEXT:
═══════════════════════════════════════════════════════════════════════════════

$piqContext

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW SO FAR:
═══════════════════════════════════════════════════════════════════════════════

$qaHistory

═══════════════════════════════════════════════════════════════════════════════
OLQs REQUIRING DEEPER ASSESSMENT:
═══════════════════════════════════════════════════════════════════════════════

$weakOLQNames ($weakOLQEnums)

These OLQs showed:
- Weak indicators in previous responses
- Insufficient evidence for assessment
- Need deeper probing for accurate scoring

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Generate $count ADAPTIVE FOLLOW-UP questions that:

1. Probe deeper into the weak OLQs: $weakOLQEnums
2. Challenge vague or generic answers from previous responses
3. Present situational scenarios requiring those specific OLQs
4. Are MORE CHALLENGING than initial questions (difficulty 4-5)
5. Reference what they said earlier to show you're listening

FOLLOW-UP STRATEGIES:
- If answer was vague: "You mentioned [X], can you give me a specific example?"
- If answer was theoretical: "Tell me about a time YOU actually did this"
- If avoiding specifics: "Walk me through exactly what YOU said and did"
- If too positive: "What was the most difficult part? What would you do differently?"

${getJsonOutputInstructionsQuestions()}
""".trimIndent()
    }

    // ===========================================
    // RESPONSE ANALYSIS PROMPT
    // ===========================================

    /**
     * Build prompt for response analysis with OLQ scoring using SSB framework.
     */
    fun buildResponseAnalysisPrompt(
        questionText: String,
        responseText: String,
        expectedOLQs: List<OLQ>,
        responseMode: String
    ): String {
        val olqList = expectedOLQs.joinToString(", ") { it.name }

        return """
You are an SSB PSYCHOLOGIST analyzing a candidate's interview response.

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW CONTEXT:
═══════════════════════════════════════════════════════════════════════════════

QUESTION ASKED: $questionText

TARGET OLQs FOR THIS QUESTION: $olqList

RESPONSE MODE: $responseMode

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE'S RESPONSE:
═══════════════════════════════════════════════════════════════════════════════

$responseText

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

${SSBPromptCore.getFactorConsistencyRules()}

${SSBPromptCore.getLimitationGuidance()}

${SSBPromptCore.getPenalizingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

${SSBPromptCore.getBoostingBehaviorsPrompt(SSBPromptCore.TestType.INTERVIEW)}

${getOLQDefinitions()}

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If response is gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **FACTOR CONSISTENCY**: Ensure scores within each factor are consistent (±1 for strict factors, ±2 for lenient)

3. **CRITICAL OLQ CHECK**: Pay special attention to the 6 critical OLQs - document clear evidence

4. **LIMITATION THRESHOLD**: Score of 8 = limitation. Flag any OLQ at 8 or above.

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze the response and provide OLQ assessment.

FOR EACH TARGET OLQ, ASSESS:
1. Score (1-10, lower is better)
2. Specific reasoning based on what they said
3. Direct evidence (exact phrases/behaviors from response)

ALSO PROVIDE:
- Overall confidence in your assessment (0-100%)
- Key insights about the candidate
- Suggested follow-up question if needed

${getJsonOutputInstructionsAnalysis()}
""".trimIndent()
    }

    // ===========================================
    // FEEDBACK GENERATION PROMPT
    // ===========================================

    /**
     * Build prompt for comprehensive feedback generation with SSB context.
     */
    fun buildFeedbackPrompt(
        piqContext: String,
        questionAnswerPairs: List<Pair<String, String>>,
        olqScores: Map<OLQ, Float>
    ): String {
        val qaHistory = questionAnswerPairs.mapIndexed { index, (q, a) ->
            "Q${index + 1}: $q\nA${index + 1}: $a"
        }.joinToString("\n\n")

        // Lower is better in SSB
        val scoresSummary = olqScores.entries
            .sortedBy { it.value }
            .joinToString("\n") { (olq, score) ->
                val assessment = when {
                    score <= 3 -> "Exceptional"
                    score <= 5 -> "Very Good"
                    score <= 6 -> "Good"
                    score <= 7 -> "Average"
                    score >= 8 -> "LIMITATION"
                    else -> "Needs Improvement"
                }
                val critical = if (olq.isCritical) " [CRITICAL]" else ""
                "- ${olq.displayName}$critical: ${"%.1f".format(score)}/10 ($assessment)"
            }

        val strongOLQs = olqScores.entries
            .filter { it.value <= 5 }
            .sortedBy { it.value }
            .take(3)
            .map { it.key.displayName }

        val weakOLQs = olqScores.entries
            .filter { it.value >= 7 }
            .sortedByDescending { it.value }
            .take(3)
            .map { "${it.key.displayName}${if (it.key.isCritical) " [CRITICAL]" else ""}" }

        val limitations = olqScores.entries
            .filter { it.value >= 8 }
            .map { it.key.displayName }

        val limitationWarning = if (limitations.isNotEmpty()) {
            "\n⚠️ LIMITATIONS DETECTED (score ≥ 8): ${limitations.joinToString(", ")}"
        } else ""

        return """
You are a SENIOR SSB PSYCHOLOGIST providing final interview feedback.

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE PROFILE:
═══════════════════════════════════════════════════════════════════════════════

$piqContext

${SSBPromptCore.getScoringScaleInstructions()}

${SSBPromptCore.getFactorContextPrompt()}

${SSBPromptCore.getCriticalQualityWarning()}

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW TRANSCRIPT:
═══════════════════════════════════════════════════════════════════════════════

$qaHistory

═══════════════════════════════════════════════════════════════════════════════
OLQ ASSESSMENT SCORES (1-10 scale, lower is better):
═══════════════════════════════════════════════════════════════════════════════

$scoresSummary
$limitationWarning

Strong Areas (score ≤ 5): ${strongOLQs.joinToString(", ").ifBlank { "None identified" }}
Areas for Development (score ≥ 7): ${weakOLQs.joinToString(", ").ifBlank { "None identified" }}

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Provide comprehensive, constructive feedback covering:

1. OVERALL PERFORMANCE SUMMARY (2-3 sentences)
   - General impression
   - Interview demeanor
   - Communication quality

2. KEY STRENGTHS (Top 3 OLQs with scores ≤ 5)
   - Cite specific examples from their responses
   - Explain how this quality manifested
   - Connect to their background if relevant

3. AREAS FOR IMPROVEMENT (OLQs with scores ≥ 7)
   - Be specific about what was lacking
   - Provide actionable improvement steps
   - Be constructive, not discouraging
   - HIGHLIGHT any CRITICAL OLQs that are weak

4. LIMITATION ANALYSIS (if any OLQ score ≥ 8)
   - Explain the severity of limitations
   - Provide focused remediation advice
   - Note if any critical OLQs are limitations

5. SSB PREPARATION RECOMMENDATIONS
   - Specific activities/exercises for weak OLQs
   - How to leverage their strengths
   - General preparation advice

6. ENCOURAGING CONCLUSION
   - Acknowledge their effort
   - Motivational closing
   - Focus on potential for growth

═══════════════════════════════════════════════════════════════════════════════
TONE REQUIREMENTS:
═══════════════════════════════════════════════════════════════════════════════

- Professional and respectful
- Constructive, not harsh
- Specific, not generic
- Encouraging but honest
- Length: 400-500 words

Write the feedback directly as plain text (not JSON).
""".trimIndent()
    }
}
