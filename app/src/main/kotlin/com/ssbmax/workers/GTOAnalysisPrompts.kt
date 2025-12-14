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

**Topic**: ${submission.topic}
**Candidate Response**: 
${submission.response}

**Word Count**: ${submission.wordCount}
**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's response against ALL 15 Officer-Like Qualities (OLQs):

1. **Effective Intelligence**: Clarity, logic, analytical thinking
2. **Reasoning Ability**: Problem-solving approach
3. **Organizing Ability**: Structure of arguments
4. **Power of Expression**: Articulation, communication clarity
5. **Social Adjustment**: Respect for diverse views
6. **Cooperation**: Collaborative tone
7. **Sense of Responsibility**: Accountability in arguments
8. **Initiative**: Leadership potential in discussion
9. **Self Confidence**: Conviction in opinions
10. **Speed of Decision**: Decisiveness in stance
11. **Ability to Influence Group**: Persuasiveness
12. **Liveliness**: Energy and enthusiasm
13. **Determination**: Firmness in viewpoint
14. **Courage**: Willingness to take bold positions
15. **Stamina**: Sustained quality throughout

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional (rare, outstanding)
- 4: Excellent (top tier)
- 5: Very Good (best common score)
- 6: Good (above average)
- 7: Average (typical performance)
- 8: Below Average (lowest acceptable)
- 9-10: Poor (usually rejected)

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Do NOT include any text like "Here is the analysis" or explanations.
Return ONLY the JSON object with exact enum names:

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "Clear analytical thinking demonstrated"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical arguments presented"},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 5, "confidence": 85, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "..."},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "..."},
    "INITIATIVE": {"score": 5, "confidence": 85, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "..."},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "..."},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
    
    private fun generateGPEPrompt(submission: GTOSubmission.GPESubmission): String {
        val solutionSection = if (!submission.solution.isNullOrBlank()) {
            """
            
            **Ideal/Suggested Scenario Solution**:
            ${submission.solution}
            """
        } else ""

        return """
You are analyzing a Group Planning Exercise (GPE) response for SSB GTO assessment.

**Scenario**: ${submission.scenario}${solutionSection}
**Candidate's Tactical Plan**:
${submission.plan}

**Character Count**: ${submission.characterCount}
**Time Spent**: ${submission.timeSpent} seconds

**GPE Assessment Focus**:
The Group Planning Exercise tests a candidate's ability to:
1. Analyze a tactical military scenario
2. Develop a practical action plan
3. Allocate resources effectively
4. Demonstrate leadership qualities
5. Consider contingencies and risks

Evaluate the candidate's tactical planning against ALL 15 Officer-Like Qualities (OLQs):

1. **Effective Intelligence**: Clarity of situation analysis, understanding of tactical requirements
2. **Reasoning Ability**: Logical approach to problem-solving, tactical thinking
3. **Organizing Ability**: Resource allocation, task sequencing, team coordination
4. **Power of Expression**: Clarity of plan communication
5. **Social Adjustment**: Team consideration in planning
6. **Cooperation**: Collaborative approach in resource sharing
7. **Sense of Responsibility**: Accountability for mission success
8. **Initiative**: Leadership in solution design, proactive thinking
9. **Self Confidence**: Decisiveness in choices
10. **Speed of Decision**: Quick assessment and planning
11. **Ability to Influence Group**: Persuasive leadership approach
12. **Liveliness**: Dynamic and creative solutions
13. **Determination**: Firmness in execution plan
14. **Courage**: Willingness to take calculated risks
15. **Stamina**: Sustained quality and thoroughness

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional (rare, outstanding tactical planning)
- 4: Excellent (top tier leadership)
- 5: Very Good (best common score for officers)
- 6: Good (above average)
- 7: Average (typical performance)
- 8: Below Average (lowest acceptable)
- 9-10: Poor (usually rejected)

**Evaluation Criteria**:
- Does the plan address the tactical scenario comprehensively?
- Are resources (personnel, equipment, time) allocated effectively?
- Does the plan show leadership and initiative?
- Is there consideration of contingencies and risks?
- Is the plan practical and achievable?
- **Compare against the Ideal Solution provided (if any) for accuracy and completeness.**

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Do NOT include any text like "Here is the analysis" or explanations.
Return ONLY the JSON object below:

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

**Topic Chosen**: ${submission.selectedTopic}
**Available Topics**: ${submission.topicChoices.joinToString(", ")}
**Speech Transcript**: 
${submission.speechTranscript}

**Word Count**: ${submission.wordCount}
**Time Spent**: ${submission.timeSpent} seconds

**Lecturette Assessment Focus**:
A Lecturette tests a candidate's ability to:
1. Speak coherently on a chosen topic for 3 minutes
2. Demonstrate subject knowledge and quick thinking
3. Communicate effectively with confidence
4. Engage and persuade the group
5. Display leadership qualities through verbal communication

Evaluate the candidate's speech against ALL 15 Officer-Like Qualities (OLQs):

1. **Effective Intelligence**: Clarity of thought, depth of topic understanding, analytical insights
2. **Reasoning Ability**: Logical flow of arguments, coherent structure
3. **Organizing Ability**: Speech structure, time management, point sequencing
4. **Power of Expression**: Fluency, articulation, vocabulary, communication clarity
5. **Social Adjustment**: Audience awareness, appropriate tone
6. **Cooperation**: Collaborative spirit in communication
7. **Sense of Responsibility**: Ownership of views, accountability in statements
8. **Initiative**: Leadership in topic selection, original insights
9. **Self Confidence**: Conviction, poise, self-assurance in delivery
10. **Speed of Decision**: Quick thinking, topic choice decisiveness
11. **Ability to Influence Group**: Persuasiveness, engagement, impact
12. **Liveliness**: Energy, enthusiasm, dynamism in delivery
13. **Determination**: Firmness in viewpoint, persistence in message
14. **Courage**: Willingness to express bold/original views
15. **Stamina**: Sustained quality and energy throughout 3 minutes

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional (rare, outstanding speakers)
- 4: Excellent (top tier communication)
- 5: Very Good (best common score for officers)
- 6: Good (above average)
- 7: Average (typical performance)
- 8: Below Average (lowest acceptable)
- 9-10: Poor (usually rejected)

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Do NOT include any text like "Here is the analysis" or explanations.
Return ONLY the JSON object with exact enum names:

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
            "**Obstacle ${solution.obstacleId}**: ${solution.solutionText}"
        }
        
        return """
You are analyzing a Progressive Group Task response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} progressive challenges
**Candidate's Solutions**: 
$solutionsText

**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's problem-solving across progressively difficult obstacles against ALL 15 OLQs.

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional, 4: Excellent, 5: Very Good
- 6: Good, 7: Average, 8: Below Average, 9-10: Poor

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Return ONLY the JSON object with exact enum names (all 15 OLQs):

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "..."},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "..."},
    "INITIATIVE": {"score": 6, "confidence": 85, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 70, "reasoning": "..."},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "..."},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
    
    private fun generateHGTPrompt(submission: GTOSubmission.HGTSubmission): String {
        return """
You are analyzing a Half Group Task response for SSB GTO assessment.

**Obstacle**: ${submission.obstacle.name} - ${submission.obstacle.description}
**Solution**: ${submission.solution.solutionText}
**Leadership Decisions**: ${submission.leadershipDecisions}

**Time Spent**: ${submission.timeSpent} seconds

Focus on leadership qualities: Initiative, Organizing Ability, Ability to Influence Group.
Evaluate ALL 15 OLQs.

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional, 4: Excellent, 5: Very Good
- 6: Good, 7: Average, 8: Below Average, 9-10: Poor

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Return ONLY the JSON object with exact enum names (all 15 OLQs):

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "..."},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "..."},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 5, "confidence": 85, "reasoning": "..."},
    "LIVELINESS": {"score": 7, "confidence": 65, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "..."},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
    
    private fun generateGORPrompt(submission: GTOSubmission.GORSubmission): String {
        return """
You are analyzing a Group Obstacle Race response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} team obstacles
**Coordination Strategy**: 
${submission.coordinationStrategy}

**Time Spent**: ${submission.timeSpent} seconds

Focus on teamwork: Cooperation, Social Adjustment, Organizing Ability, Stamina.
Evaluate ALL 15 OLQs.

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional, 4: Excellent, 5: Very Good
- 6: Good, 7: Average, 8: Below Average, 9-10: Poor

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Return ONLY the JSON object with exact enum names (all 15 OLQs):

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 5, "confidence": 85, "reasoning": "..."},
    "COOPERATION": {"score": 5, "confidence": 90, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "..."},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "..."},
    "LIVELINESS": {"score": 6, "confidence": 75, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "..."},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "STAMINA": {"score": 5, "confidence": 85, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
    
    private fun generateIOPrompt(submission: GTOSubmission.IOSubmission): String {
        return """
You are analyzing Individual Obstacles response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} individual challenges
**Overall Approach**: 
${submission.approach}

**Time Spent**: ${submission.timeSpent} seconds

Focus on individual qualities: Courage, Determination, Self Confidence, Stamina.
Evaluate ALL 15 OLQs.

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional, 4: Excellent, 5: Very Good
- 6: Good, 7: Average, 8: Below Average, 9-10: Poor

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Return ONLY the JSON object with exact enum names (all 15 OLQs):

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "..."},
    "COOPERATION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 80, "reasoning": "..."},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 5, "confidence": 85, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 7, "confidence": 70, "reasoning": "..."},
    "LIVELINESS": {"score": 7, "confidence": 70, "reasoning": "..."},
    "DETERMINATION": {"score": 5, "confidence": 90, "reasoning": "..."},
    "COURAGE": {"score": 5, "confidence": 90, "reasoning": "..."},
    "STAMINA": {"score": 5, "confidence": 85, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
    
    private fun generateCTPrompt(submission: GTOSubmission.CTSubmission): String {
        return """
You are analyzing a Command Task response for SSB GTO assessment.

**Scenario**: ${submission.scenario}
**Obstacle**: ${submission.obstacle.name}
**Command Decisions**: ${submission.commandDecisions}
**Resource Allocation**: ${submission.resourceAllocation}

**Time Spent**: ${submission.timeSpent} seconds

Focus on command qualities: Initiative, Organizing Ability, Speed of Decision, Courage.
Evaluate ALL 15 OLQs.

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional, 4: Excellent, 5: Very Good
- 6: Good, 7: Average, 8: Below Average, 9-10: Poor

**CRITICAL: Your response MUST be ONLY valid JSON with NO explanatory text before or after.**
Return ONLY the JSON object with exact enum names (all 15 OLQs):

{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 5, "confidence": 85, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 7, "confidence": 70, "reasoning": "..."},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 5, "confidence": 85, "reasoning": "..."},
    "INITIATIVE": {"score": 5, "confidence": 90, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 5, "confidence": 85, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 6, "confidence": 75, "reasoning": "..."},
    "LIVELINESS": {"score": 7, "confidence": 70, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 80, "reasoning": "..."},
    "COURAGE": {"score": 5, "confidence": 85, "reasoning": "..."},
    "STAMINA": {"score": 6, "confidence": 80, "reasoning": "..."}
  }
}
        """.trimIndent()
    }
}
