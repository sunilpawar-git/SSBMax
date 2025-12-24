package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.WATSubmission
import com.ssbmax.core.domain.model.SRTSubmission
import com.ssbmax.core.domain.model.SDTSubmission

/**
 * Optimized Psychology Test Prompts for Gemini AI
 * 
 * Token-efficient prompts matching GTO format (~900 tokens each).
 * Reduced from ~1,800 tokens to avoid TPM rate limits.
 * 
 * All prompts enforce JSON-only responses with all 15 OLQs scored.
 */
object PsychologyTestPrompts {

    /**
     * Generate TAT analysis prompt (optimized for token efficiency)
     */
    fun generateTATAnalysisPrompt(submission: TATSubmission): String {
        val storiesText = submission.stories.mapIndexed { index, story ->
            "Story ${index + 1}: ${story.story}"
        }.joinToString("\n\n")

        return """
You are analyzing TAT (Thematic Apperception Test) stories for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
TAT STORIES (${submission.stories.size} stories):
═══════════════════════════════════════════════════════════════════════════════

$storiesText

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense
2. REASONING_ABILITY: Logical thinking, problem-solving
3. ORGANIZING_ABILITY: Planning, systematic approach
4. POWER_OF_EXPRESSION: Communication clarity
5. SOCIAL_ADJUSTMENT: Adaptability, flexibility
6. COOPERATION: Teamwork, helping others
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability
8. INITIATIVE: Proactive action, self-starting
9. SELF_CONFIDENCE: Composure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making
11. INFLUENCE_GROUP: Leadership, persuasion
12. LIVELINESS: Energy, optimism
13. DETERMINATION: Persistence, goal-oriented
14. COURAGE: Facing fears, standing up for beliefs
15. STAMINA: Endurance, resilience

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (UNIFIED - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5: Very Good/Excellent (BEST possible score - rare)
6: Good (Above average)
7: Average (Typical performance)
8: Poor (Needs improvement)
9: Fail (Gibberish/Irrelevant/Blank)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If stories are gibberish, random characters, single words,
   or clearly irrelevant → Assign score 9 for ALL OLQs, confidence 100,
   reasoning: "Response appears to be gibberish or irrelevant"

2. **LENGTH CHECK**: If average story length < 20 words → Score 8-9

3. **CONSERVATIVE SCORING**: When averaging across 12 stories, bias towards
   the lower side (worse scores). Do NOT be lenient.

4. **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 5-9), confidence (integer 0-100), reasoning (string)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Clear problem-solving in 7/12 stories"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical flow in most stories"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 75, "reasoning": "Heroes plan before acting"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Coherent narratives"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 85, "reasoning": "Heroes adapt to situations"},
    "COOPERATION": {"score": 4, "confidence": 90, "reasoning": "Strong teamwork themes"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Heroes take ownership"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive heroes"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 75, "reasoning": "Mostly confident heroes"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 80, "reasoning": "Quick decisions shown"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Leadership in 6/12 stories"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Optimistic endings"},
    "DETERMINATION": {"score": 4, "confidence": 90, "reasoning": "Persistent heroes"},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "Heroes face fears"},
    "STAMINA": {"score": 6, "confidence": 70, "reasoning": "Endurance themes"}
  }
}
        """.trimIndent()
    }

    /**
     * Generate WAT analysis prompt (optimized for token efficiency)
     */
    fun generateWATAnalysisPrompt(submission: WATSubmission): String {
        val responsesText = submission.responses.take(60).mapIndexed { index, response ->
            "${index + 1}. ${response.word} → ${response.response} (${response.timeTakenSeconds}s)"
        }.joinToString("\n")

        return """
You are analyzing WAT (Word Association Test) responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
WAT RESPONSES (60 word associations):
═══════════════════════════════════════════════════════════════════════════════

$responsesText

Average response time: ${submission.averageResponseTime}s

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense
2. REASONING_ABILITY: Logical thinking, problem-solving
3. ORGANIZING_ABILITY: Planning, systematic approach
4. POWER_OF_EXPRESSION: Communication clarity
5. SOCIAL_ADJUSTMENT: Adaptability, flexibility
6. COOPERATION: Teamwork, helping others
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability
8. INITIATIVE: Proactive action, self-starting
9. SELF_CONFIDENCE: Composure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making
11. INFLUENCE_GROUP: Leadership, persuasion
12. LIVELINESS: Energy, optimism
13. DETERMINATION: Persistence, goal-oriented
14. COURAGE: Facing fears, standing up for beliefs
15. STAMINA: Endurance, resilience

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (UNIFIED - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5: Very Good/Excellent (BEST possible score - rare)
6: Good (Above average)
7: Average (Typical performance)
8: Poor (Needs improvement)
9: Fail (Gibberish/Irrelevant/Blank)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If responses are gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **CONSERVATIVE SCORING**: Bias towards the lower side (worse scores). Do NOT be lenient.

3. **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "Creative associations shown"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical patterns"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Structured thinking"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Adequate vocabulary"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 75, "reasoning": "Social words present"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Team-oriented responses"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Accountable language"},
    "INITIATIVE": {"score": 6, "confidence": 85, "reasoning": "Action words used"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Positive associations"},
    "SPEED_OF_DECISION": {"score": 5, "confidence": 85, "reasoning": "Quick response time"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "Leadership words"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Optimistic tone"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Persistent themes"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Bold words used"},
    "STAMINA": {"score": 6, "confidence": 75, "reasoning": "Sustained quality"}
  }
}
        """.trimIndent()
    }

    /**
     * Generate SRT analysis prompt (optimized for token efficiency)
     */
    fun generateSRTAnalysisPrompt(submission: SRTSubmission): String {
        val responsesText = submission.responses.take(60).mapIndexed { index, response ->
            "${index + 1}. ${response.situation}\n   Response: ${response.response}"
        }.joinToString("\n\n")

        return """
You are analyzing SRT (Situation Reaction Test) responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
SRT RESPONSES (60 situations):
═══════════════════════════════════════════════════════════════════════════════

$responsesText

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense
2. REASONING_ABILITY: Logical thinking, problem-solving
3. ORGANIZING_ABILITY: Planning, systematic approach
4. POWER_OF_EXPRESSION: Communication clarity
5. SOCIAL_ADJUSTMENT: Adaptability, flexibility
6. COOPERATION: Teamwork, helping others
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability
8. INITIATIVE: Proactive action, self-starting
9. SELF_CONFIDENCE: Composure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making
11. INFLUENCE_GROUP: Leadership, persuasion
12. LIVELINESS: Energy, optimism
13. DETERMINATION: Persistence, goal-oriented
14. COURAGE: Facing fears, standing up for beliefs
15. STAMINA: Endurance, resilience

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (UNIFIED - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5: Very Good/Excellent (BEST possible score - rare)
6: Good (Above average)
7: Average (Typical performance)
8: Poor (Needs improvement)
9: Fail (Gibberish/Irrelevant/Blank)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If responses are gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **CONSERVATIVE SCORING**: Bias towards the lower side (worse scores). Do NOT be lenient.

3. **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "Practical solutions"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical approach"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Planning evident"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Clear communication"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 75, "reasoning": "Flexible responses"},
    "COOPERATION": {"score": 5, "confidence": 85, "reasoning": "Team solutions"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Takes ownership"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive actions"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Decisive responses"},
    "SPEED_OF_DECISION": {"score": 5, "confidence": 85, "reasoning": "Quick decisions"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Leadership shown"},
    "LIVELINESS": {"score": 6, "confidence": 75, "reasoning": "Positive approach"},
    "DETERMINATION": {"score": 5, "confidence": 85, "reasoning": "Persistent effort"},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "Stands up for right"},
    "STAMINA": {"score": 6, "confidence": 75, "reasoning": "Sustained quality"}
  }
}
        """.trimIndent()
    }

    /**
     * Generate SD (Self Description) analysis prompt (optimized for token efficiency)
     */
    fun generateSDAnalysisPrompt(submission: SDTSubmission): String {
        val descriptionsText = submission.responses.mapIndexed { index, response ->
            val perspective = when (index) {
                0 -> "Parents' Opinion"
                1 -> "Teachers/Seniors' Opinion"
                2 -> "Friends' Opinion"
                3 -> "Own Opinion"
                else -> "Response ${index + 1}"
            }
            "$perspective: ${response.answer}"
        }.joinToString("\n\n")

        return """
You are analyzing Self Description Test responses for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
SELF DESCRIPTION RESPONSES:
═══════════════════════════════════════════════════════════════════════════════

$descriptionsText

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense
2. REASONING_ABILITY: Logical thinking, problem-solving
3. ORGANIZING_ABILITY: Planning, systematic approach
4. POWER_OF_EXPRESSION: Communication clarity
5. SOCIAL_ADJUSTMENT: Adaptability, flexibility
6. COOPERATION: Teamwork, helping others
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability
8. INITIATIVE: Proactive action, self-starting
9. SELF_CONFIDENCE: Composure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making
11. INFLUENCE_GROUP: Leadership, persuasion
12. LIVELINESS: Energy, optimism
13. DETERMINATION: Persistence, goal-oriented
14. COURAGE: Facing fears, standing up for beliefs
15. STAMINA: Endurance, resilience

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (UNIFIED - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5: Very Good/Excellent (BEST possible score - rare)
6: Good (Above average)
7: Average (Typical performance)
8: Poor (Needs improvement)
9: Fail (Gibberish/Irrelevant/Blank)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If responses are gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **CONSERVATIVE SCORING**: Bias towards the lower side (worse scores). Do NOT be lenient.

3. **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Shows self-awareness"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical self-view"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Structured responses"},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 80, "reasoning": "Clear communication"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 75, "reasoning": "Awareness of others"},
    "COOPERATION": {"score": 5, "confidence": 85, "reasoning": "Mentions helping others"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Accountable language"},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "Proactive traits"},
    "SELF_CONFIDENCE": {"score": 5, "confidence": 85, "reasoning": "Optimistic self-view"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Decisive language"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Leadership mentioned"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Positive outlook"},
    "DETERMINATION": {"score": 5, "confidence": 85, "reasoning": "Goal-oriented language"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Acknowledges challenges"},
    "STAMINA": {"score": 6, "confidence": 75, "reasoning": "Resilience mentioned"}
  }
}
        """.trimIndent()
    }

    /**
     * Generate PPDT analysis prompt (optimized for token efficiency)
     */
    fun generatePPDTAnalysisPrompt(submission: com.ssbmax.core.domain.model.PPDTSubmission): String {
        return """
You are analyzing PPDT (Picture Perception & Description Test) story for SSB assessment.

═══════════════════════════════════════════════════════════════════════════════
PPDT STORY:
═══════════════════════════════════════════════════════════════════════════════

${submission.story}

(Characters: ${submission.charactersCount}, Time: ${submission.writingTimeTakenMinutes} minutes)

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense
2. REASONING_ABILITY: Logical thinking, problem-solving
3. ORGANIZING_ABILITY: Planning, systematic approach
4. POWER_OF_EXPRESSION: Communication clarity
5. SOCIAL_ADJUSTMENT: Adaptability, flexibility
6. COOPERATION: Teamwork, helping others
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability
8. INITIATIVE: Proactive action, self-starting
9. SELF_CONFIDENCE: Composure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making
11. INFLUENCE_GROUP: Leadership, persuasion
12. LIVELINESS: Energy, optimism
13. DETERMINATION: Persistence, goal-oriented
14. COURAGE: Facing fears, standing up for beliefs
15. STAMINA: Endurance, resilience

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (UNIFIED - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

5: Very Good/Excellent (BEST possible score - rare)
6: Good (Above average)
7: Average (Typical performance)
8: Poor (Needs improvement)
9: Fail (Gibberish/Irrelevant/Blank)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL VALIDATION (MUST CHECK FIRST):
═══════════════════════════════════════════════════════════════════════════════

1. **GARBAGE DETECTION**: If responses are gibberish, random characters, or clearly irrelevant
   → Assign score 9 for ALL OLQs, confidence 100, reasoning: "Response appears to be gibberish or irrelevant"

2. **CONSERVATIVE SCORING**: Bias towards the lower side (worse scores). Do NOT be lenient.

3. **SCORE RANGE**: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Clear perception"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical flow"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Structured narrative"},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 80, "reasoning": "Good vocabulary"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 75, "reasoning": "Characters adapt"},
    "COOPERATION": {"score": 5, "confidence": 85, "reasoning": "Teamwork shown"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Hero takes ownership"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive hero"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Confident actions"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Quick decisions"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Leadership evident"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Optimistic ending"},
    "DETERMINATION": {"score": 5, "confidence": 85, "reasoning": "Persistent hero"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Hero faces challenges"},
    "STAMINA": {"score": 6, "confidence": 75, "reasoning": "Sustained effort"}
  }
}
        """.trimIndent()
    }
}
