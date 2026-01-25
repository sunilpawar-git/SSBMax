package com.ssbmax.core.domain.prompts

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.scoring.SSBScoringRules

/**
 * SSB Prompt Core - Single Source of Truth (SSOT) for all SSB scoring prompts.
 * 
 * This object provides standardized prompt sections that can be composed
 * into complete prompts for Psychology, GTO, and Interview tests.
 * 
 * Key SSB Concepts encoded:
 * - 15 OLQs organized in 4 Factors
 * - 1-10 scoring scale (lower = better)
 * - Limitation threshold of 8
 * - 6 Critical OLQs with special handling
 * - Factor consistency rules (±1 or ±2 tick)
 * - Factor II auto-reject rule
 */
object SSBPromptCore {

    // ===========================================
    // FACTOR CONTEXT PROMPTS
    // ===========================================

    /**
     * Get comprehensive context about all 4 SSB factors.
     * Use this to educate the AI about the SSB scoring framework.
     */
    fun getFactorContextPrompt(): String = """
## SSB Factor Structure

The 15 Officer-Like Qualities (OLQs) are organized into 4 Factors:

### Factor I: Planning & Organizing (Intellectual Qualities)
- Effective Intelligence (EI): Ability to perceive, understand and adapt
- Reasoning Ability (RA): Logical thinking and problem-solving [CRITICAL]
- Organizing Ability (OA): Planning, prioritizing, and structuring tasks
- Power of Expression (PoE): Clear and effective communication

### Factor II: Social Adjustment (MOST CRITICAL FACTOR)
⚠️ This factor is paramount - if overall Factor II score = 8, candidate is automatically rejected.
- Social Adjustment (SA): Ability to adapt socially [CRITICAL]
- Cooperation (CO-OP): Working effectively with others [CRITICAL]
- Sense of Responsibility (SoR): Taking ownership and accountability [CRITICAL]

### Factor III: Social Effectiveness (Dynamic Qualities)
- Initiative (INI): Taking proactive action
- Self-Confidence (SC): Belief in one's abilities
- Speed of Decision (SoD): Quick and appropriate decision-making
- Ability to Influence Group (AIG): Leadership and persuasion
- Liveliness (LIV): Energy, optimism, and morale-building [CRITICAL]

### Factor IV: Dynamic (Character & Physical)
- Determination (DET): Persistence and resolve
- Courage (COU): Facing challenges without fear [CRITICAL]
- Stamina (STA): Physical and mental endurance
""".trimIndent()

    /**
     * Get context for a specific factor/category.
     */
    fun getFactorContextForCategory(category: OLQCategory): String {
        val olqs = OLQ.getByCategory(category)
        val olqList = olqs.joinToString(", ") { 
            if (it.isCritical) "${it.displayName} [CRITICAL]" else it.displayName
        }
        
        val criticalNote = if (category.isCriticalFactor) {
            "\n⚠️ CRITICAL FACTOR: If overall score for this factor = 8, candidate is automatically rejected."
        } else ""
        
        return """
### ${category.ssbFactorName} (Factor ${toRoman(category.ssbFactorNumber)})
OLQs: $olqList
Consistency: Scores within this factor should vary by at most ±${category.maxTickVariation} tick(s).$criticalNote
""".trimIndent()
    }

    // ===========================================
    // CRITICAL QUALITY WARNINGS
    // ===========================================

    /**
     * Get comprehensive warning about critical OLQs.
     * Include this in prompts to ensure AI pays special attention.
     */
    fun getCriticalQualityWarning(): String = """
## Critical Quality Alert ⚠️

The following 6 OLQs are CRITICAL - a score of 8 or higher on these qualities 
is a serious concern and may lead to rejection:

1. **Reasoning Ability (RA)** - Factor I
   Core analytical thinking required for officer duties

2. **Social Adjustment (SA)** - Factor II
   Essential for team integration and unit cohesion

3. **Cooperation (CO-OP)** - Factor II
   Fundamental for military teamwork

4. **Sense of Responsibility (SoR)** - Factor II
   Critical for leadership accountability

5. **Liveliness (LIV)** - Factor III
   Important for maintaining morale and motivation

6. **Courage (COU)** - Factor IV
   Essential for leadership under pressure

**AUTOMATIC REJECTION RULE:**
If the overall Factor II (Social Adjustment) average score is 8 or higher, 
the candidate is automatically NOT RECOMMENDED, regardless of other scores.
""".trimIndent()

    /**
     * Get specific warning for a single OLQ (if critical).
     * Returns empty string for non-critical OLQs.
     */
    fun getCriticalQualityWarningForOLQ(olq: OLQ): String {
        if (!olq.isCritical) return ""
        
        return """
⚠️ CRITICAL QUALITY: ${olq.displayName}
This is one of the 6 critical OLQs. A score of 8 or higher indicates a limitation 
that may significantly impact the candidate's recommendation.
""".trimIndent()
    }

    // ===========================================
    // FACTOR CONSISTENCY RULES
    // ===========================================

    /**
     * Get rules about score consistency within factors.
     */
    fun getFactorConsistencyRules(): String = """
## Factor Consistency Rules

SSB assessors expect scores within each factor to be internally consistent:

### Strict Factors (±1 tick maximum variation):
- **Factor I (Planning & Organizing)**: EI, RA, OA, PoE should have similar scores
- **Factor II (Social Adjustment)**: SA, CO-OP, SoR should have similar scores

### Lenient Factors (±2 tick maximum variation):
- **Factor III (Social Effectiveness)**: INI, SC, SoD, AIG, LIV can have slightly more variation
- **Factor IV (Dynamic)**: DET, COU, STA can have slightly more variation

**WHY THIS MATTERS:**
If a candidate shows EI=3 but RA=7, this is a red flag indicating:
- Possible assessment error
- Need for re-evaluation
- Inconsistent behavioral evidence

Always ensure scores within a factor are logically consistent based on observed behaviors.
""".trimIndent()

    /**
     * Get consistency rule for a specific category.
     */
    fun getConsistencyRuleForCategory(category: OLQCategory): String {
        val strictness = if (category.maxTickVariation == 1) "strict" else "lenient"
        return """
${category.ssbFactorName}: Maximum ±${category.maxTickVariation} tick variation allowed ($strictness consistency).
""".trimIndent()
    }

    // ===========================================
    // SCORING SCALE INSTRUCTIONS
    // ===========================================

    /**
     * Get comprehensive scoring scale instructions.
     */
    fun getScoringScaleInstructions(): String = """
## SSB Scoring Scale (1-10)

⚠️ IMPORTANT: Lower scores indicate BETTER performance (inverse scale).

### Score Meanings:
- **1-3**: Exceptional (rare, outstanding performance - bell curve tail)
- **4**: Excellent (top tier, uncommon)
- **5**: Very Good (best common score)
- **6**: Good (above average)
- **7**: Average (typical performance)
- **8**: Below Average / LIMITATION (lowest acceptable, concerning)
- **9-10**: Poor (usually results in rejection)

### Distribution Guidelines:
Following a normal bell curve distribution:
- Scores of 1-3: ~5% of candidates (exceptional)
- Scores of 4-5: ~20% of candidates (very good to excellent)
- Scores of 6-7: ~50% of candidates (good to average)
- Scores of 8+: ~25% of candidates (below average to poor)

### Key Threshold:
**Score of 8 = LIMITATION**
- NDA candidates: Maximum 4 limitations allowed
- OTA/Graduate candidates: Maximum 7 limitations allowed
""".trimIndent()

    // ===========================================
    // LIMITATION GUIDANCE
    // ===========================================

    /**
     * Get guidance about limitation thresholds.
     */
    fun getLimitationGuidance(): String = """
## Limitation System

A **limitation** is defined as any OLQ score of 8 or higher.

### Entry Type Limits:
- **NDA (National Defence Academy)**: Maximum 4 limitations
  - Younger candidates, more potential for development
  - Stricter standards due to longer service commitment
  
- **OTA (Officers Training Academy)**: Maximum 7 limitations
  - Graduate entry, shorter service
  - More lenient threshold

- **Graduate Entry**: Maximum 7 limitations
  - Similar to OTA standards

### Impact of Limitations:
1. Each limitation is a concern that assessors note
2. Multiple limitations compound the concern
3. Exceeding the maximum = NOT RECOMMENDED
4. Critical OLQ limitations carry extra weight
""".trimIndent()

    // ===========================================
    // COMPLETE CONTEXT ASSEMBLY
    // ===========================================

    /**
     * Get complete SSB context for comprehensive prompts.
     * Use this when you need the full SSB scoring framework.
     */
    fun getCompleteSSBContext(): String = """
# SSB Scoring Framework - Complete Reference

${getScoringScaleInstructions()}

${getFactorContextPrompt()}

${getCriticalQualityWarning()}

${getFactorConsistencyRules()}

${getLimitationGuidance()}
""".trimIndent()

    /**
     * Get OLQ-specific prompt section including factor context and critical warnings.
     */
    fun getOLQSpecificPrompt(olq: OLQ): String {
        val categoryContext = getFactorContextForCategory(olq.category)
        val criticalWarning = getCriticalQualityWarningForOLQ(olq)
        
        return buildString {
            appendLine("## Assessing: ${olq.displayName}")
            appendLine()
            appendLine(categoryContext)
            if (criticalWarning.isNotEmpty()) {
                appendLine()
                appendLine(criticalWarning)
            }
            appendLine()
            appendLine("### Scoring for ${olq.displayName}:")
            appendLine("- Consider behaviors that demonstrate this quality")
            appendLine("- Use the 1-10 scale (lower = better)")
            appendLine("- Ensure consistency with other ${olq.category.ssbFactorName} qualities")
        }
    }

    // ===========================================
    // UTILITY FUNCTIONS
    // ===========================================

    /**
     * Convert number to Roman numeral (1-4 for SSB factors).
     */
    private fun toRoman(number: Int): String = when (number) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        else -> number.toString()
    }

    /**
     * Get penalizing behaviors guidance for a specific test type.
     * This provides test-specific negative indicators.
     */
    fun getPenalizingBehaviorsPrompt(testType: TestType): String = when (testType) {
        TestType.TAT -> """
### Penalizing Indicators (TAT):
- Negative/pessimistic story endings
- Passive protagonist who avoids action
- Blame-shifting to others
- Giving up when facing obstacles
- Isolation or withdrawal themes
- Lack of problem-solving attempts
""".trimIndent()

        TestType.WAT -> """
### Penalizing Indicators (WAT):
- Negative word associations
- Aggressive or violent responses
- Self-deprecating patterns
- Avoidance or escapist themes
- Repetitive or unimaginative responses
- Blank or "don't know" responses
""".trimIndent()

        TestType.SRT -> """
### Penalizing Indicators (SRT):
- Avoiding the situation entirely
- Blaming others for the problem
- Waiting for someone else to act
- Giving up without trying
- Unethical or dishonest solutions
- Panic or emotional breakdown responses
""".trimIndent()

        TestType.SDT -> """
### Penalizing Indicators (SDT):
- Major discrepancies between self-view and others' view
- Unrealistic self-assessment
- Avoiding mention of weaknesses
- Defensive about criticism
- Lack of self-awareness
- Inconsistency between stated goals and behaviors
""".trimIndent()

        TestType.PPDT -> """
### Penalizing Indicators (PPDT):
- Passive story with no hero action
- Negative/tragic endings
- Hero gives up or fails
- No clear problem identification
- Lack of social interaction
- Blame or escape as resolution
""".trimIndent()

        TestType.GTO -> """
### Penalizing Indicators (GTO):
- Not volunteering for tasks
- Dominating without listening
- Criticizing team members
- Giving up on difficult obstacles
- Working alone, ignoring team
- Taking credit for others' work
""".trimIndent()

        TestType.INTERVIEW -> """
### Penalizing Indicators (Interview):
- Inconsistent answers
- Blame-shifting for failures
- Lack of clarity about goals
- Defensive responses to probing
- Poor knowledge of chosen field
- Unrealistic self-assessment
""".trimIndent()
    }

    /**
     * Get boosting behaviors guidance for a specific test type.
     * This provides test-specific positive indicators.
     */
    fun getBoostingBehaviorsPrompt(testType: TestType): String = when (testType) {
        TestType.TAT -> """
### Boosting Indicators (TAT):
- Protagonist takes initiative
- Constructive problem-solving
- Optimistic but realistic outcomes
- Team collaboration themes
- Taking responsibility for actions
- Learning from setbacks
""".trimIndent()

        TestType.WAT -> """
### Boosting Indicators (WAT):
- Positive, action-oriented associations
- Leadership and teamwork themes
- Creative and diverse responses
- Quick, confident responses
- Constructive and goal-oriented
- Social and collaborative themes
""".trimIndent()

        TestType.SRT -> """
### Boosting Indicators (SRT):
- Immediate practical action
- Taking personal responsibility
- Involving appropriate help
- Ethical and honest approach
- Calm, systematic response
- Follow-through and completion
""".trimIndent()

        TestType.SDT -> """
### Boosting Indicators (SDT):
- Accurate self-awareness
- Acknowledged weaknesses with improvement plans
- Alignment between self-view and others' view
- Clear, realistic goals
- Openness to feedback
- Consistent narrative across all perspectives
""".trimIndent()

        TestType.PPDT -> """
### Boosting Indicators (PPDT):
- Clear hero identification
- Active problem-solving
- Positive outcome through effort
- Social engagement and teamwork
- Initiative and leadership
- Responsible conclusion
""".trimIndent()

        TestType.GTO -> """
### Boosting Indicators (GTO):
- Volunteering for challenges
- Encouraging team members
- Listening and incorporating ideas
- Persisting through difficulty
- Sharing credit with team
- Supporting struggling members
""".trimIndent()

        TestType.INTERVIEW -> """
### Boosting Indicators (Interview):
- Consistent, coherent answers
- Owning failures with lessons learned
- Clear motivation for service
- Thoughtful self-reflection
- Strong domain knowledge
- Realistic aspirations with plans
""".trimIndent()
    }

    /**
     * Test types for which prompts can be generated.
     */
    enum class TestType {
        TAT, WAT, SRT, SDT, PPDT, GTO, INTERVIEW
    }
}
