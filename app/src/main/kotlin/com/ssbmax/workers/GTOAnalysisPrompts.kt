package com.ssbmax.workers

import com.ssbmax.core.domain.model.gto.GTOSubmission

/**
 * Prompt generation for GTO test analysis
 */
object GTOAnalysisPrompts {
    
    fun generateAnalysisPrompt(submission: GTOSubmission): String {
        return when (submission) {
            is GTOSubmission.GDSubmission -> generateGDPrompt(submission)
            is GTOSubmission.GPESubmission -> generateGPEPrompt(submission)
            is GTOSubmission.LecturetteSubmission -> generateLecturettePrompt(submission)
            is GTOSubmission.PGTSubmission -> generatePGTPrompt(submission)
            is GTOSubmission.HGTSubmission -> generateHGTPrompt(submission)
            is GTOSubmission.GORSubmission -> generateGORPrompt(submission)
            is GTOSubmission.IOSubmission -> generateIOPrompt(submission)
            is GTOSubmission.CTSubmission -> generateCTPrompt(submission)
        }
    }
    
    private fun generateGDPrompt(submission: GTOSubmission.GDSubmission): String {
        return """
You are analyzing a Group Discussion response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GD SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Topic: ${submission.topic}

Candidate Response:
${submission.response}

Word Count: ${submission.wordCount}
Time Spent: ${submission.timeSpent} seconds

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Clarity, logic, analytical thinking
2. REASONING_ABILITY: Problem-solving approach
3. ORGANIZING_ABILITY: Structure of arguments
4. POWER_OF_EXPRESSION: Articulation, communication clarity
5. SOCIAL_ADJUSTMENT: Respect for diverse views
6. COOPERATION: Collaborative tone
7. SENSE_OF_RESPONSIBILITY: Accountability in arguments
8. INITIATIVE: Leadership potential in discussion
9. SELF_CONFIDENCE: Conviction in opinions
10. SPEED_OF_DECISION: Decisiveness in stance
11. INFLUENCE_GROUP: Persuasiveness
12. LIVELINESS: Energy and enthusiasm
13. DETERMINATION: Firmness in viewpoint
14. COURAGE: Willingness to take bold positions
15. STAMINA: Sustained quality throughout

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (CRITICAL - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

1-3: Exceptional (rare, outstanding)
4: Excellent (top tier)
5: Very Good (best common score)
6: Good (above average)
7: Average (typical performance)
8: Below Average (lowest acceptable)
9-10: Poor (usually rejected)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS - READ CAREFULLY:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 1-10), confidence (integer 0-100), reasoning (string)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Your response must match this EXACTLY):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "Clear analytical thinking demonstrated"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical arguments presented"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Well-structured response"},
    "POWER_OF_EXPRESSION": {"score": 5, "confidence": 85, "reasoning": "Excellent articulation"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Adequate respect for diverse views"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Collaborative tone evident"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Accountable in arguments"},
    "INITIATIVE": {"score": 5, "confidence": 85, "reasoning": "Strong leadership potential"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Conviction in opinions shown"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Decisive stance taken"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "Persuasive approach"},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "Moderate energy level"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Firm viewpoint maintained"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Willing to take bold positions"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Sustained quality throughout"}
  }
}
        """.trimIndent()
    }
    
    private fun generateGPEPrompt(submission: GTOSubmission.GPESubmission): String {
        val solutionSection = if (!submission.solution.isNullOrBlank()) {
            """

Ideal/Suggested Scenario Solution:
${submission.solution}
            """
        } else ""

        return """
You are analyzing a Group Planning Exercise (GPE) response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GPE SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Scenario: ${submission.scenario}${solutionSection}

Candidate's Tactical Plan:
${submission.plan}

Character Count: ${submission.characterCount}
Time Spent: ${submission.timeSpent} seconds

═══════════════════════════════════════════════════════════════════════════════
GPE ASSESSMENT FOCUS:
═══════════════════════════════════════════════════════════════════════════════

The Group Planning Exercise tests a candidate's ability to:
1. Analyze a tactical military scenario
2. Develop a practical action plan
3. Allocate resources effectively
4. Demonstrate leadership qualities
5. Consider contingencies and risks

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Clarity of situation analysis, understanding of tactical requirements
2. REASONING_ABILITY: Logical approach to problem-solving, tactical thinking
3. ORGANIZING_ABILITY: Resource allocation, task sequencing, team coordination
4. POWER_OF_EXPRESSION: Clarity of plan communication
5. SOCIAL_ADJUSTMENT: Team consideration in planning
6. COOPERATION: Collaborative approach in resource sharing
7. SENSE_OF_RESPONSIBILITY: Accountability for mission success
8. INITIATIVE: Leadership in solution design, proactive thinking
9. SELF_CONFIDENCE: Decisiveness in choices
10. SPEED_OF_DECISION: Quick assessment and planning
11. INFLUENCE_GROUP: Persuasive leadership approach
12. LIVELINESS: Dynamic and creative solutions
13. DETERMINATION: Firmness in execution plan
14. COURAGE: Willingness to take calculated risks
15. STAMINA: Sustained quality and thoroughness

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (CRITICAL - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

1-3: Exceptional (rare, outstanding tactical planning)
4: Excellent (top tier leadership)
5: Very Good (best common score for officers)
6: Good (above average)
7: Average (typical performance)
8: Below Average (lowest acceptable)
9-10: Poor (usually rejected)

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CHECKLIST:
═══════════════════════════════════════════════════════════════════════════════

- Does the plan address the tactical scenario comprehensively?
- Are resources (personnel, equipment, time) allocated effectively?
- Does the plan show leadership and initiative?
- Is there consideration of contingencies and risks?
- Is the plan practical and achievable?
- Compare against the Ideal Solution provided (if any) for accuracy

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS - READ CAREFULLY:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 1-10), confidence (integer 0-100), reasoning (string)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Your response must match this EXACTLY):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Clear tactical analysis, identified key challenges"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical sequence of actions"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 90, "reasoning": "Excellent resource allocation and team coordination"},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 75, "reasoning": "Clear communication of plan"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Adequate team consideration"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Collaborative approach evident"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Strong accountability for mission"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive leadership demonstrated"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Decisive choices made"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Quick tactical assessment"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "Leadership approach shown"},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "Creative solutions proposed"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Firm execution plan"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Calculated risks considered"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Thorough throughout the plan"}
  }
}
        """.trimIndent()
    }
    
    private fun generateLecturettePrompt(submission: GTOSubmission.LecturetteSubmission): String {
        return """
You are analyzing a Lecturette (3-minute speech) for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
LECTURETTE SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Topic Chosen: ${submission.selectedTopic}
Available Topics: ${submission.topicChoices.joinToString(", ")}

Speech Transcript:
${submission.speechTranscript}

Word Count: ${submission.wordCount}
Time Spent: ${submission.timeSpent} seconds

═══════════════════════════════════════════════════════════════════════════════
LECTURETTE ASSESSMENT FOCUS:
═══════════════════════════════════════════════════════════════════════════════

A Lecturette tests a candidate's ability to:
1. Speak coherently on a chosen topic for 3 minutes
2. Demonstrate subject knowledge and quick thinking
3. Communicate effectively with confidence
4. Engage and persuade the group
5. Display leadership qualities through verbal communication

═══════════════════════════════════════════════════════════════════════════════
EVALUATION CRITERIA - ALL 15 OLQs (MANDATORY):
═══════════════════════════════════════════════════════════════════════════════

1. EFFECTIVE_INTELLIGENCE: Clarity of thought, depth of topic understanding, analytical insights
2. REASONING_ABILITY: Logical flow of arguments, coherent structure
3. ORGANIZING_ABILITY: Speech structure, time management, point sequencing
4. POWER_OF_EXPRESSION: Fluency, articulation, vocabulary, communication clarity
5. SOCIAL_ADJUSTMENT: Audience awareness, appropriate tone
6. COOPERATION: Collaborative spirit in communication
7. SENSE_OF_RESPONSIBILITY: Ownership of views, accountability in statements
8. INITIATIVE: Leadership in topic selection, original insights
9. SELF_CONFIDENCE: Conviction, poise, self-assurance in delivery
10. SPEED_OF_DECISION: Quick thinking, topic choice decisiveness
11. INFLUENCE_GROUP: Persuasiveness, engagement, impact
12. LIVELINESS: Energy, enthusiasm, dynamism in delivery
13. DETERMINATION: Firmness in viewpoint, persistence in message
14. COURAGE: Willingness to express bold/original views
15. STAMINA: Sustained quality and energy throughout 3 minutes

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (CRITICAL - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

1-3: Exceptional (rare, outstanding speakers)
4: Excellent (top tier communication)
5: Very Good (best common score for officers)
6: Good (above average)
7: Average (typical performance)
8: Below Average (lowest acceptable)
9-10: Poor (usually rejected)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS - READ CAREFULLY:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 1-10), confidence (integer 0-100), reasoning (string)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Your response must match this EXACTLY):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 85, "reasoning": "Clear understanding and analytical insights on topic"},
    "REASONING_ABILITY": {"score": 6, "confidence": 80, "reasoning": "Logical flow with coherent arguments"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Well-structured speech with good time management"},
    "POWER_OF_EXPRESSION": {"score": 5, "confidence": 90, "reasoning": "Excellent fluency, articulation, and communication"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Good audience awareness"},
    "COOPERATION": {"score": 7, "confidence": 65, "reasoning": "Collaborative tone in delivery"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 75, "reasoning": "Accountable statements with ownership"},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "Good topic choice with original insights"},
    "SELF_CONFIDENCE": {"score": 5, "confidence": 85, "reasoning": "Strong conviction and poise throughout"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Quick topic selection and thinking"},
    "INFLUENCE_GROUP": {"score": 5, "confidence": 90, "reasoning": "Highly persuasive and engaging delivery"},
    "LIVELINESS": {"score": 5, "confidence": 85, "reasoning": "Dynamic energy and enthusiasm evident"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Firm viewpoint maintained"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Expressed bold views confidently"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Sustained quality throughout 3 minutes"}
  }
}
        """.trimIndent()
    }
    
    private fun generatePGTPrompt(submission: GTOSubmission.PGTSubmission): String {
        val solutionsText = submission.solutions.joinToString("\n\n") { solution ->
            "Obstacle ${solution.obstacleId}: ${solution.solutionText}"
        }

        return """
You are analyzing a Progressive Group Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
PGT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: ${submission.obstacles.size} progressive challenges

Candidate's Solutions:
$solutionsText

Time Spent: ${submission.timeSpent} seconds

═══════════════════════════════════════════════════════════════════════════════
SSB SCORING SCALE (CRITICAL - LOWER IS BETTER):
═══════════════════════════════════════════════════════════════════════════════

1-3: Exceptional, 4: Excellent, 5: Very Good
6: Good, 7: Average, 8: Below Average, 9-10: Poor

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS - READ CAREFULLY:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json or ``` markers)
3. NO explanatory text before or after the JSON
4. ALL 15 OLQs MUST be present (failure to include all 15 will cause analysis to fail)
5. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
6. Your entire response should START with { and END with }
7. Each OLQ must have: score (integer 1-10), confidence (integer 0-100), reasoning (string)

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT (Your response must match this EXACTLY):
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "Problem-solving approach analyzed"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical thinking demonstrated"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Systematic approach shown"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Communication clarity adequate"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Team dynamics considered"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Collaborative approach evident"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Accountability shown"},
    "INITIATIVE": {"score": 6, "confidence": 85, "reasoning": "Proactive solutions provided"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Decisive actions taken"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Timely decisions made"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "Leadership potential shown"},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "Energy level adequate"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Persistence demonstrated"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Willingness to tackle challenges"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Sustained effort throughout"}
  }
}
        """.trimIndent()
    }
    
    private fun generateHGTPrompt(submission: GTOSubmission.HGTSubmission): String {
        return """
You are analyzing a Half Group Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
HGT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacle: ${submission.obstacle.name} - ${submission.obstacle.description}
Solution: ${submission.solution.solutionText}
Leadership Decisions: ${submission.leadershipDecisions}
Time Spent: ${submission.timeSpent} seconds

Focus: Leadership qualities (Initiative, Organizing Ability, Influence Group)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json markers)
3. ALL 15 OLQs MUST be present
4. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
5. Your response should START with { and END with }
6. SSB Scoring: 1-10 where LOWER is BETTER

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "Analytical approach shown"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical thinking demonstrated"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "Excellent leadership coordination"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Clear communication"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Team awareness adequate"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Collaborative approach"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Accountability shown"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Strong proactive leadership"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Decisive actions"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Timely decisions"},
    "INFLUENCE_GROUP": {"score": 5, "confidence": 85, "reasoning": "Strong group influence"},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "Moderate energy"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Persistent effort"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Willing to take risks"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Sustained performance"}
  }
}
        """.trimIndent()
    }

    private fun generateGORPrompt(submission: GTOSubmission.GORSubmission): String {
        return """
You are analyzing a Group Obstacle Race response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
GOR SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: ${submission.obstacles.size} team obstacles
Coordination Strategy:
${submission.coordinationStrategy}
Time Spent: ${submission.timeSpent} seconds

Focus: Teamwork qualities (Cooperation, Social Adjustment, Organizing Ability, Stamina)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json markers)
3. ALL 15 OLQs MUST be present
4. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
5. Your response should START with { and END with }
6. SSB Scoring: 1-10 where LOWER is BETTER

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 75, "reasoning": "Tactical thinking shown"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical approach"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "Excellent team coordination"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Communication adequate"},
    "SOCIAL_ADJUSTMENT": {"score": 5, "confidence": 85, "reasoning": "Strong team integration"},
    "COOPERATION": {"score": 5, "confidence": 90, "reasoning": "Exceptional teamwork"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Reliable team player"},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "Proactive support"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Confident actions"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Quick thinking"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Team influence shown"},
    "LIVELINESS": {"score": 6, "confidence": 75, "reasoning": "Good energy level"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Persistent effort"},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "Physical challenges faced"},
    "STAMINA": {"score": 5, "confidence": 85, "reasoning": "Excellent endurance"}
  }
}
        """.trimIndent()
    }

    private fun generateIOPrompt(submission: GTOSubmission.IOSubmission): String {
        return """
You are analyzing Individual Obstacles response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
IO SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Obstacles: ${submission.obstacles.size} individual challenges
Overall Approach:
${submission.approach}
Time Spent: ${submission.timeSpent} seconds

Focus: Individual qualities (Courage, Determination, Self Confidence, Stamina)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json markers)
3. ALL 15 OLQs MUST be present
4. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
5. Your response should START with { and END with }
6. SSB Scoring: 1-10 where LOWER is BETTER

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 75, "reasoning": "Problem-solving shown"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Tactical thinking"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Planning evident"},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "Communication basic"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Individual task focus"},
    "COOPERATION": {"score": 7, "confidence": 70, "reasoning": "Not applicable to IO"},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "Personal accountability"},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "Self-directed approach"},
    "SELF_CONFIDENCE": {"score": 5, "confidence": 85, "reasoning": "Strong self-assurance"},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "Quick decisions"},
    "INFLUENCE_GROUP": {"score": 7, "confidence": 70, "reasoning": "Not applicable to IO"},
    "LIVELINESS": {"score": 7, "confidence": 70, "reasoning": "Moderate energy"},
    "DETERMINATION": {"score": 5, "confidence": 90, "reasoning": "Exceptional persistence"},
    "COURAGE": {"score": 5, "confidence": 90, "reasoning": "Strong physical courage"},
    "STAMINA": {"score": 5, "confidence": 85, "reasoning": "Excellent endurance"}
  }
}
        """.trimIndent()
    }

    private fun generateCTPrompt(submission: GTOSubmission.CTSubmission): String {
        return """
You are analyzing a Command Task response for SSB GTO assessment.

═══════════════════════════════════════════════════════════════════════════════
CT SUBMISSION DATA:
═══════════════════════════════════════════════════════════════════════════════

Scenario: ${submission.scenario}
Obstacle: ${submission.obstacle.name}
Command Decisions: ${submission.commandDecisions}
Resource Allocation: ${submission.resourceAllocation}
Time Spent: ${submission.timeSpent} seconds

Focus: Command qualities (Initiative, Organizing Ability, Speed of Decision, Courage)

═══════════════════════════════════════════════════════════════════════════════
CRITICAL INSTRUCTIONS:
═══════════════════════════════════════════════════════════════════════════════

1. Return ONLY a single JSON object
2. NO markdown code blocks (no ```json markers)
3. ALL 15 OLQs MUST be present
4. Use EXACT enum names: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, etc.
5. Your response should START with { and END with }
6. SSB Scoring: 1-10 where LOWER is BETTER

═══════════════════════════════════════════════════════════════════════════════
OUTPUT FORMAT:
═══════════════════════════════════════════════════════════════════════════════

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "Tactical analysis shown"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical command decisions"},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "Excellent resource management"},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 75, "reasoning": "Clear instructions"},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "Team dynamics considered"},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "Collaborative leadership"},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "Strong mission ownership"},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "Proactive command approach"},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "Decisive leadership"},
    "SPEED_OF_DECISION": {"score": 5, "confidence": 85, "reasoning": "Quick tactical decisions"},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "Command authority shown"},
    "LIVELINESS": {"score": 7, "confidence": 70, "reasoning": "Moderate energy"},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "Persistent execution"},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "Bold command decisions"},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "Sustained focus"}
  }
}
        """.trimIndent()
    }
}
