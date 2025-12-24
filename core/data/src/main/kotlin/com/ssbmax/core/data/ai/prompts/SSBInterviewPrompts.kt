package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.interview.OLQ

/**
 * SSB Interview Prompt Templates for Gemini AI
 *
 * Contains:
 * - Comprehensive OLQ definitions with behavioral indicators
 * - PIQ-to-OLQ mapping guidance
 * - Question generation prompts with personalization
 * - Response analysis prompts
 * - Difficulty-based question strategies
 *
 * These prompts are designed to elicit high-quality, personalized
 * SSB interview questions that assess Officer-Like Qualities.
 */
object SSBInterviewPrompts {

    /**
     * Complete OLQ definitions with behavioral indicators
     * Used to help AI understand what each quality means and how to assess it
     */
    const val OLQ_DEFINITIONS = """
OFFICER-LIKE QUALITIES (OLQs) - Definitions & Behavioral Indicators:

INTELLECTUAL QUALITIES (Factor-I):
1. EFFECTIVE_INTELLIGENCE
   Definition: Practical wisdom, ability to grasp situations quickly and take appropriate action
   Indicators: Common sense solutions, quick understanding, practical approach to problems
   Questions to reveal: Problem-solving scenarios, real-life challenges faced

2. REASONING_ABILITY
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

SOCIAL QUALITIES (Factor-II):
5. SOCIAL_ADJUSTMENT
   Definition: Adaptability to different social situations, mixing with diverse people
   Indicators: Comfort in varied settings, respect for others, flexibility
   Questions to reveal: Experiences with different groups, handling cultural differences

6. COOPERATION
   Definition: Team player, willingness to help, putting group before self
   Indicators: Collaborative spirit, helping others, accepting others' ideas
   Questions to reveal: Team experiences, times they helped others, conflict resolution

7. INFLUENCE_GROUP
   Definition: Natural leadership, ability to convince and motivate others
   Indicators: Others follow their suggestions, can persuade without authority
   Questions to reveal: Leadership experiences, times they changed group opinion

DYNAMIC QUALITIES (Factor-III):
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

11. DETERMINATION
    Definition: Persistence, goal-oriented, doesn't give up easily
    Indicators: Long-term goal pursuit, overcoming obstacles, bouncing back
    Questions to reveal: Long-term goals, times they persisted despite difficulties

12. COURAGE
    Definition: Physical and moral courage, standing up for beliefs
    Indicators: Speaking truth, facing fears, defending principles
    Questions to reveal: Times they stood up for something, faced fears

CHARACTER & PHYSICAL QUALITIES (Factor-IV):
13. SENSE_OF_RESPONSIBILITY
    Definition: Accountability, reliability, duty-consciousness
    Indicators: Owning mistakes, completing commitments, dependability
    Questions to reveal: Responsibilities held, times they owned up to mistakes

14. STAMINA
    Definition: Physical and mental endurance, resilience
    Indicators: Sustained effort, handling stress, physical fitness
    Questions to reveal: Endurance challenges, stressful periods handled

15. LIVELINESS
    Definition: Energy, enthusiasm, positive outlook
    Indicators: Active participation, optimistic attitude, energetic demeanor
    Questions to reveal: Hobbies, how they energize others, daily routine
"""

    /**
     * PIQ section to OLQ mapping guidance
     * Helps AI understand which PIQ fields reveal which OLQs
     */
    const val PIQ_TO_OLQ_MAPPING = """
PIQ SECTIONS → OLQ ASSESSMENT MAPPING:

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

SSB JOURNEY:
- First attempt → Assess baseline OLQs
- Repeat attempt → DETERMINATION (why trying again), learning shown
- Multiple attempts → Strong DETERMINATION, but probe SELF_CONFIDENCE

SELF-ASSESSMENT:
- Strengths stated → Verify through behavioral questions
- Weaknesses acknowledged → EFFECTIVE_INTELLIGENCE (self-awareness)
- Why defense → SENSE_OF_RESPONSIBILITY, COURAGE, DETERMINATION
"""

    /**
     * Get difficulty level description for prompt
     */
    fun getDifficultyDescription(difficulty: Int): String = when (difficulty) {
        1 -> "Icebreaker - Warm, easy opening questions to build rapport"
        2 -> "Basic Probing - Simple background exploration, direct questions"
        3 -> "Moderate Challenge - Situational questions, 'what would you do' scenarios"
        4 -> "Deep Probing - Challenging assumptions, value-based dilemmas"
        5 -> "Stress Testing - Rapid fire, contradiction exploration, pressure scenarios"
        else -> "Standard difficulty"
    }

    /**
     * Build comprehensive question generation prompt
     *
     * @param piqContext Comprehensive PIQ context from PIQDataMapper
     * @param count Number of questions to generate
     * @param difficulty Difficulty level (1-5)
     * @param targetOLQs Optional specific OLQs to focus on
     * @return Complete prompt for Gemini AI
     */
    fun buildQuestionGenerationPrompt(
        piqContext: String,
        count: Int,
        difficulty: Int,
        targetOLQs: List<OLQ>? = null
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

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
PIQ TO OLQ MAPPING (use this to connect candidate's background to OLQs):
═══════════════════════════════════════════════════════════════════════════════

$PIQ_TO_OLQ_MAPPING

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
   ✗ NO questions about future plans (too easy to rehearse)

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
    "difficultyLevel": $difficulty,
    "expectedResponseTime": "2-3 minutes"
  }
]

IMPORTANT: 
- Return ONLY the JSON array, no markdown formatting
- Use exact OLQ names from the list (e.g., EFFECTIVE_INTELLIGENCE, not "Effective Intelligence")
- Generate exactly $count questions
        """.trimIndent()
    }

    /**
     * Build prompt for adaptive follow-up question generation
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

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW SO FAR:
═══════════════════════════════════════════════════════════════════════════════

$qaHistory

═══════════════════════════════════════════════════════════════════════════════
OLQs REQUIRING DEEPER ASSESSMENT:
═══════════════════════════════════════════════════════════════════════════════

$weakOLQNames

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

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON array):
═══════════════════════════════════════════════════════════════════════════════

[
  {
    "id": "followup-1",
    "questionText": "Your adaptive follow-up question?",
    "targetOLQs": ["$weakOLQEnums"],
    "reasoning": "Why this follow-up is needed based on previous responses",
    "referencesAnswer": "Which previous answer this builds upon",
    "difficultyLevel": 4
  }
]
        """.trimIndent()
    }

    /**
     * Build prompt for response analysis with OLQ scoring
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

═══════════════════════════════════════════════════════════════════════════════
OLQ SCORING REFERENCE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
SCORING SCALE (UNIFIED SSB - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5   = Very Good/Excellent (BEST possible score - rare)
6   = Good (Above average)
7   = Average (Typical performance)
8   = Poor (Needs improvement)
9   = Fail (Gibberish/Irrelevant/Blank)

CRITICAL VALIDATION:
- **GARBAGE DETECTION**: If response is gibberish or clearly irrelevant → Score 9
- **CONSERVATIVE SCORING**: Bias towards lower side (worse scores)
- **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.
- Use decimal scores for precision within 5-9 range (e.g., 5.5, 6.2)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze the response and provide OLQ assessment.

FOR EACH TARGET OLQ, ASSESS:
1. Score (1-10 with decimals, lower is better)
2. Specific reasoning based on what they said
3. Direct evidence (exact phrases/behaviors from response)

ALSO PROVIDE:
- Overall confidence in your assessment (0-100%)
- Key insights about the candidate
- Suggested follow-up question if needed

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Return ONLY valid JSON):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": [
    {
      "olq": "OLQ_NAME",
      "score": 5.5,
      "reasoning": "Why this score - specific to their response",
      "evidence": ["Exact phrases from response that support this score"]
    }
  ],
  "overallConfidence": 75,
  "keyInsights": [
    "Notable observations about the candidate based on this response"
  ],
  "suggestedFollowUp": "A follow-up question to probe further if needed"
}

IMPORTANT:
- Score ONLY the OLQs listed in TARGET OLQs
- Lower scores mean BETTER performance
- Be specific with evidence - quote from their response
- If response doesn't provide enough data for an OLQ, note this
        """.trimIndent()
    }

    /**
     * Build prompt for comprehensive feedback generation
     */
    fun buildFeedbackPrompt(
        piqContext: String,
        questionAnswerPairs: List<Pair<String, String>>,
        olqScores: Map<OLQ, Float>
    ): String {
        val qaHistory = questionAnswerPairs.mapIndexed { index, (q, a) ->
            "Q${index + 1}: $q\nA${index + 1}: $a"
        }.joinToString("\n\n")

        val scoresSummary = olqScores.entries
            .sortedBy { it.value } // Lower is better in SSB
            .joinToString("\n") { (olq, score) ->
                val assessment = when {
                    score <= 3 -> "Excellent"
                    score <= 5 -> "Good"
                    score <= 7 -> "Average"
                    else -> "Needs Improvement"
                }
                "- ${olq.displayName}: ${"%.1f".format(score)}/10 ($assessment)"
            }

        val strongOLQs = olqScores.entries
            .filter { it.value <= 5 }
            .sortedBy { it.value }
            .take(3)
            .map { it.key.displayName }

        val weakOLQs = olqScores.entries
            .filter { it.value > 6 }
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.displayName }

        return """
You are a SENIOR SSB PSYCHOLOGIST providing final interview feedback.

═══════════════════════════════════════════════════════════════════════════════
CANDIDATE PROFILE:
═══════════════════════════════════════════════════════════════════════════════

$piqContext

═══════════════════════════════════════════════════════════════════════════════
INTERVIEW TRANSCRIPT:
═══════════════════════════════════════════════════════════════════════════════

$qaHistory

═══════════════════════════════════════════════════════════════════════════════
OLQ ASSESSMENT SCORES (1-10 scale, lower is better):
═══════════════════════════════════════════════════════════════════════════════

$scoresSummary

Strong Areas: ${strongOLQs.joinToString(", ").ifBlank { "None identified" }}
Areas for Development: ${weakOLQs.joinToString(", ").ifBlank { "None identified" }}

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Provide comprehensive, constructive feedback covering:

1. OVERALL PERFORMANCE SUMMARY (2-3 sentences)
   - General impression
   - Interview demeanor
   - Communication quality

2. KEY STRENGTHS (Top 3 OLQs)
   - Cite specific examples from their responses
   - Explain how this quality manifested
   - Connect to their background if relevant

3. AREAS FOR IMPROVEMENT (3-4 OLQs)
   - Be specific about what was lacking
   - Provide actionable improvement steps
   - Be constructive, not discouraging

4. SSB PREPARATION RECOMMENDATIONS
   - Specific activities/exercises for weak OLQs
   - How to leverage their strengths
   - General preparation advice

5. ENCOURAGING CONCLUSION
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

