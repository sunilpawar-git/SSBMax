package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.WATSubmission
import com.ssbmax.core.domain.model.SRTSubmission
import com.ssbmax.core.domain.model.SDTSubmission

/**
 * Psychology Test Prompt Templates for Gemini AI
 *
 * Generates OLQ-based analysis prompts for:
 * - TAT (Thematic Apperception Test)
 * - WAT (Word Association Test)
 * - SRT (Situation Reaction Test)
 * - SD (Self Description Test)
 *
 * All prompts enforce JSON-only responses with all 15 OLQs scored.
 */
object PsychologyTestPrompts {

    /**
     * Complete OLQ definitions for psychology test analysis
     */
    private const val OLQ_DEFINITIONS = """
OFFICER-LIKE QUALITIES (OLQs) - Definitions for Psychology Test Analysis:

INTELLECTUAL QUALITIES:
1. EFFECTIVE_INTELLIGENCE: Practical wisdom, quick grasp of situations, common sense solutions
2. REASONING_ABILITY: Logical thinking, cause-effect analysis, structured problem-solving
3. ORGANIZING_ABILITY: Systematic planning, prioritization, methodical approach
4. POWER_OF_EXPRESSION: Clear communication, vocabulary, coherent narrative

SOCIAL QUALITIES:
5. SOCIAL_ADJUSTMENT: Adaptability, mixing with diverse people, flexibility
6. COOPERATION: Team player, helping others, collaborative spirit
7. SENSE_OF_RESPONSIBILITY: Accountability, reliability, duty-consciousness

DYNAMIC QUALITIES:
8. INITIATIVE: Proactive action, self-starter, volunteering without being asked
9. SELF_CONFIDENCE: Belief in abilities, composure under pressure, positive self-image
10. SPEED_OF_DECISION: Quick decision-making, action-oriented, comfortable with uncertainty
11. INFLUENCE_GROUP: Natural leadership, ability to convince and motivate
12. LIVELINESS: Energy, enthusiasm, optimistic outlook, positive attitude

CHARACTER QUALITIES:
13. DETERMINATION: Persistence, goal-oriented, not giving up despite obstacles
14. COURAGE: Physical and moral courage, standing up for beliefs, facing fears
15. STAMINA: Physical and mental endurance, resilience, sustained effort
"""

    /**
     * Generate TAT analysis prompt for OLQ scoring
     *
     * Analyzes 11-12 TAT stories for OLQ patterns:
     * - Character heroes (proactive vs reactive)
     * - Problem-solving approach
     * - Positive endings vs pessimistic outcomes
     * - Leadership themes
     * - Helping behaviors
     */
    fun generateTATAnalysisPrompt(submission: TATSubmission): String {
        val storiesText = submission.stories.mapIndexed { index, story ->
            """
Story ${index + 1}:
${story.story}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
You are a SENIOR SSB PSYCHOLOGIST analyzing TAT (Thematic Apperception Test) stories.

═══════════════════════════════════════════════════════════════════════════════
TAT STORIES (${submission.stories.size} stories):
═══════════════════════════════════════════════════════════════════════════════

$storiesText

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
TAT ANALYSIS PRINCIPLES:
═══════════════════════════════════════════════════════════════════════════════

POSITIVE INDICATORS (Lower OLQ scores 3-5):
- Heroes take PROACTIVE action (not waiting for others) → INITIATIVE, SPEED_OF_DECISION
- Heroes help others, lead groups → COOPERATION, INFLUENCE_GROUP
- Stories show planning before action → ORGANIZING_ABILITY, EFFECTIVE_INTELLIGENCE
- Heroes face challenges with courage → COURAGE, DETERMINATION
- Positive, optimistic endings → LIVELINESS, SELF_CONFIDENCE
- Heroes express thoughts clearly in dialogue → POWER_OF_EXPRESSION
- Social harmony, teamwork themes → SOCIAL_ADJUSTMENT, COOPERATION
- Heroes take responsibility for problems → SENSE_OF_RESPONSIBILITY

NEGATIVE INDICATORS (Higher OLQ scores 7-9):
- Passive heroes waiting for others to act → INITIATIVE (8-9)
- Pessimistic, tragic endings → LIVELINESS (8-9), SELF_CONFIDENCE (7-8)
- Heroes avoid problems or run away → COURAGE (8-9)
- Selfish heroes ignoring others → COOPERATION (8-9)
- Confused, illogical plot progression → REASONING_ABILITY (7-8)
- Giving up easily when facing obstacles → DETERMINATION (8-9)
- Violence, aggression without justification → SOCIAL_ADJUSTMENT (8-9)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze ALL ${submission.stories.size} TAT stories and score ALL 15 OLQs.

Use SSB scoring scale (1-10, LOWER IS BETTER):
- 1-3: Exceptional (consistent strong positive indicators across multiple stories)
- 4-5: Good (mostly positive indicators, few neutral/negative)
- 6-7: Average (mixed indicators, typical performance)
- 8-10: Poor (mostly negative indicators, concerning patterns)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown (no ```json markers)
3. NO explanatory text before or after
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your response should START with { and END with }
7. Look for patterns ACROSS ALL STORIES, not just one story

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Stories show practical problem-solving in 7/12 stories with common sense approach"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical cause-effect in most stories, some plot inconsistencies in stories 3,8"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 75, "reasoning": "Heroes plan before acting in 8/12 stories"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Coherent narratives but limited vocabulary, repetitive phrasing"},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 85, "reasoning": "Diverse social settings, heroes adapt in 9/12 stories"},
    "COOPERATION": {"score": 4, "confidence": 90, "reasoning": "Strong teamwork themes in 10/12 stories, heroes help others consistently"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Heroes take ownership in 8/12 stories, some avoidance in stories 5,11"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive heroes in 9/12 stories, self-starting behavior"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 75, "reasoning": "Mostly confident heroes, some self-doubt in stories 4,9"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 80, "reasoning": "Quick decisions in 7/12 stories, some hesitation"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Leadership shown in 6/12 stories, moderate persuasion themes"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Optimistic endings in 10/12 stories, positive energy"},
    "DETERMINATION": {"score": 4, "confidence": 90, "reasoning": "Persistent heroes overcoming obstacles in 10/12 stories"},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "Heroes face fears in 8/12 stories, moral courage shown"},
    "STAMINA": {"score": 6, "confidence": 70, "reasoning": "Endurance themes in 5/12 stories, moderate resilience"}
  },
  "overallScore": 5.5,
  "overallRating": "Good",
  "strengths": ["Strong cooperation and teamwork themes", "Persistent problem-solving", "Optimistic outlook"],
  "weaknesses": ["Limited vocabulary", "Occasional passive heroes", "Inconsistent leadership themes"],
  "recommendations": ["Practice varied vocabulary", "Focus on proactive decision-making", "Develop more leadership scenarios"],
  "aiConfidence": 82
}
        """.trimIndent()
    }

    /**
     * Generate WAT analysis prompt for OLQ scoring
     *
     * Analyzes 60 word associations for OLQ indicators:
     * - Positive/negative ratio → LIVELINESS, SELF_CONFIDENCE
     * - Speed of response → SPEED_OF_DECISION
     * - Creative associations → INITIATIVE, EFFECTIVE_INTELLIGENCE
     * - Helping/team words → COOPERATION
     */
    fun generateWATAnalysisPrompt(submission: WATSubmission): String {
        val responsesText = submission.responses.take(60).mapIndexed { index, response ->
            "${index + 1}. ${response.word} → ${response.response} (${response.timeTakenSeconds}s)"
        }.joinToString("\n")

        val avgTime = submission.averageResponseTime

        return """
You are a SENIOR SSB PSYCHOLOGIST analyzing WAT (Word Association Test) responses.

═══════════════════════════════════════════════════════════════════════════════
WAT RESPONSES (60 word associations):
═══════════════════════════════════════════════════════════════════════════════

$responsesText

Average response time: ${avgTime}s

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
WAT ANALYSIS PRINCIPLES:
═══════════════════════════════════════════════════════════════════════════════

POSITIVE INDICATORS (Lower OLQ scores):
- POSITIVE ASSOCIATIONS (>60% positive words) → LIVELINESS (3-5), SELF_CONFIDENCE (4-6)
- QUICK RESPONSES (avg <12s) → SPEED_OF_DECISION (3-5)
- CREATIVE/UNIQUE responses (not cliché) → INITIATIVE (4-6), EFFECTIVE_INTELLIGENCE (4-6)
- HELPING words (assist, support, help, team) → COOPERATION (3-5)
- ACTION words (lead, organize, plan) → ORGANIZING_ABILITY (4-6), INITIATIVE (4-6)
- COURAGEOUS words (brave, fearless, bold) → COURAGE (3-5)
- PERSISTENT words (continue, persist, strive) → DETERMINATION (3-5)
- SOCIAL words (friend, team, group) → SOCIAL_ADJUSTMENT (4-6)

NEGATIVE INDICATORS (Higher OLQ scores):
- NEGATIVE ASSOCIATIONS (>40% negative) → LIVELINESS (7-9), SELF_CONFIDENCE (7-8)
- SLOW RESPONSES (avg >14s) → SPEED_OF_DECISION (7-9)
- REPETITIVE patterns (same response type) → INITIATIVE (7-8), EFFECTIVE_INTELLIGENCE (7-8)
- VIOLENT words (kill, destroy, fight) → SOCIAL_ADJUSTMENT (8-9)
- PASSIVE words (wait, watch, rest) → INITIATIVE (8-9)
- GIVING UP words (quit, surrender, stop) → DETERMINATION (8-9)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze all 60 WAT responses and score ALL 15 OLQs.

Calculate:
1. Positive/negative/neutral ratio
2. Response speed patterns
3. Thematic patterns (leadership, helping, courage, etc.)
4. Creativity vs cliché ratio
5. Unique vs repeated response types

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown (no ```json markers)
3. NO explanatory text before or after
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (use same JSON structure as TAT example above):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": X, "confidence": Y, "reasoning": "..."},
    ... (all 15 OLQs) ...
  },
  "overallScore": X.X,
  "overallRating": "...",
  "strengths": ["...", "...", "..."],
  "weaknesses": ["...", "...", "..."],
  "recommendations": ["...", "...", "..."],
  "aiConfidence": XX
}
        """.trimIndent()
    }

    /**
     * Generate SRT analysis prompt for OLQ scoring
     *
     * Analyzes 60 situation reactions for OLQ evidence:
     * - Proactive responses → INITIATIVE, SPEED_OF_DECISION
     * - Helping others → COOPERATION, SOCIAL_ADJUSTMENT
     * - Taking charge → INFLUENCE_GROUP, ORGANIZING_ABILITY
     * - Standing up for right → COURAGE, SENSE_OF_RESPONSIBILITY
     */
    fun generateSRTAnalysisPrompt(submission: SRTSubmission): String {
        val responsesText = submission.responses.take(60).mapIndexed { index, response ->
            """
${index + 1}. SITUATION: ${response.situation}
   RESPONSE: ${response.response}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
You are a SENIOR SSB PSYCHOLOGIST analyzing SRT (Situation Reaction Test) responses.

═══════════════════════════════════════════════════════════════════════════════
SRT RESPONSES (60 situations):
═══════════════════════════════════════════════════════════════════════════════

$responsesText

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
SRT ANALYSIS PRINCIPLES:
═══════════════════════════════════════════════════════════════════════════════

POSITIVE INDICATORS (Lower OLQ scores):
- PROACTIVE action ("I will do X") → INITIATIVE (3-5), SPEED_OF_DECISION (4-6)
- HELPING others without being asked → COOPERATION (3-5), SENSE_OF_RESPONSIBILITY (4-6)
- TAKING CHARGE in crisis → INFLUENCE_GROUP (4-6), ORGANIZING_ABILITY (4-6)
- STANDING UP for right/truth → COURAGE (3-5), SENSE_OF_RESPONSIBILITY (3-5)
- QUICK decisions under pressure → SPEED_OF_DECISION (3-5)
- PLANNING before acting → ORGANIZING_ABILITY (4-6), EFFECTIVE_INTELLIGENCE (4-6)
- TEAM solutions (involving others) → COOPERATION (3-5), SOCIAL_ADJUSTMENT (4-6)
- PERSISTENT approach (not giving up) → DETERMINATION (3-5)
- CONFIDENT responses (no hesitation) → SELF_CONFIDENCE (4-6)

NEGATIVE INDICATORS (Higher OLQ scores):
- PASSIVE responses ("I will wait/watch") → INITIATIVE (8-9)
- AVOIDING responsibility → SENSE_OF_RESPONSIBILITY (8-9)
- RUNNING AWAY from problems → COURAGE (8-9)
- BLAMING others → COOPERATION (7-8), SENSE_OF_RESPONSIBILITY (7-8)
- AGGRESSIVE solutions (violence) → SOCIAL_ADJUSTMENT (8-9)
- GIVING UP easily → DETERMINATION (8-9)
- SLOW/hesitant decisions → SPEED_OF_DECISION (7-9)
- SELFISH responses (only self-interest) → COOPERATION (8-9)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze all 60 SRT responses and score ALL 15 OLQs.

Look for patterns:
1. Proactive vs reactive responses
2. Helping vs selfish tendencies
3. Leadership vs following
4. Courage vs avoidance
5. Quick vs hesitant decisions
6. Planning vs impulsive action

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown (no ```json markers)
3. NO explanatory text before or after
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (use same JSON structure as previous examples):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": X, "confidence": Y, "reasoning": "Practical solutions in X/60 situations..."},
    ... (all 15 OLQs) ...
  },
  "overallScore": X.X,
  "overallRating": "...",
  "strengths": ["...", "...", "..."],
  "weaknesses": ["...", "...", "..."],
  "recommendations": ["...", "...", "..."],
  "aiConfidence": XX
}
        """.trimIndent()
    }

    /**
     * Generate SD (Self Description) analysis prompt for OLQ scoring
     *
     * Analyzes self-description for OLQ self-awareness:
     * - Acknowledging weaknesses → EFFECTIVE_INTELLIGENCE (maturity)
     * - Optimistic self-view → SELF_CONFIDENCE, LIVELINESS
     * - Goal-oriented language → DETERMINATION, INITIATIVE
     * - Self-awareness depth → EFFECTIVE_INTELLIGENCE
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
You are a SENIOR SSB PSYCHOLOGIST analyzing Self Description Test responses.

═══════════════════════════════════════════════════════════════════════════════
SELF DESCRIPTION RESPONSES:
═══════════════════════════════════════════════════════════════════════════════

$descriptionsText

═══════════════════════════════════════════════════════════════════════════════
OLQ REFERENCE GUIDE:
═══════════════════════════════════════════════════════════════════════════════

$OLQ_DEFINITIONS

═══════════════════════════════════════════════════════════════════════════════
SELF DESCRIPTION ANALYSIS PRINCIPLES:
═══════════════════════════════════════════════════════════════════════════════

POSITIVE INDICATORS (Lower OLQ scores):
- ACKNOWLEDGING WEAKNESSES maturely → EFFECTIVE_INTELLIGENCE (3-5)
- OPTIMISTIC self-view with realism → SELF_CONFIDENCE (4-6), LIVELINESS (4-6)
- GOAL-ORIENTED language → DETERMINATION (3-5), INITIATIVE (4-6)
- AWARENESS of others' perspectives → SOCIAL_ADJUSTMENT (4-6)
- BALANCED view (not all positive or negative) → EFFECTIVE_INTELLIGENCE (4-6)
- CONCRETE examples of qualities → POWER_OF_EXPRESSION (4-6)
- LEADERSHIP themes in descriptions → INFLUENCE_GROUP (4-6)
- HELPING others mentioned → COOPERATION (3-5)

NEGATIVE INDICATORS (Higher OLQ scores):
- ONLY POSITIVE traits (no self-awareness) → EFFECTIVE_INTELLIGENCE (7-8)
- VERY NEGATIVE self-view → SELF_CONFIDENCE (7-9), LIVELINESS (7-9)
- BLAMING circumstances → SENSE_OF_RESPONSIBILITY (7-8)
- VAGUE descriptions (no examples) → POWER_OF_EXPRESSION (7-8)
- PASSIVE language ("I want to be" vs "I am") → INITIATIVE (7-8)
- FOCUS only on self (no mention of others) → COOPERATION (7-8)

═══════════════════════════════════════════════════════════════════════════════
YOUR TASK:
═══════════════════════════════════════════════════════════════════════════════

Analyze all 4 self-description responses and score ALL 15 OLQs.

Compare:
1. Consistency across 4 perspectives
2. Self-awareness level (realistic vs delusional)
3. Optimism vs pessimism
4. Maturity in acknowledging weaknesses
5. Goal-orientation and drive

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown (no ```json markers)
3. NO explanatory text before or after
4. ALL 15 OLQs MUST be present
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your response should START with { and END with }

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (use same JSON structure):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": X, "confidence": Y, "reasoning": "Shows self-awareness by..."},
    ... (all 15 OLQs) ...
  },
  "overallScore": X.X,
  "overallRating": "...",
  "strengths": ["...", "...", "..."],
  "weaknesses": ["...", "...", "..."],
  "recommendations": ["...", "...", "..."],
  "aiConfidence": XX
}
        """.trimIndent()
    }
}
