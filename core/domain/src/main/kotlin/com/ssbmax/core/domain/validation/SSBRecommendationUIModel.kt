package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.scoring.EntryType

/**
 * UI-friendly model for displaying SSB recommendation in result screens.
 * 
 * This is a presentation layer model that transforms [OLQScoreValidationResult]
 * into display-ready values for the [SSBRecommendationBanner] composable.
 */
data class SSBRecommendationUIModel(
    /** Primary recommendation outcome */
    val recommendation: RecommendationOutcome,
    
    /** Display text for recommendation (e.g., "RECOMMENDED", "NOT RECOMMENDED") */
    val recommendationText: String,
    
    /** Short subtitle explaining the recommendation */
    val subtitleText: String,
    
    /** Number of limitations (OLQs with score >= 8) */
    val limitationCount: Int,
    
    /** Maximum allowed limitations for entry type */
    val maxLimitations: Int,
    
    /** Whether limitations are within acceptable range */
    val limitationsOk: Boolean,
    
    /** Whether any critical OLQs have limitations */
    val hasCriticalWeakness: Boolean,
    
    /** Names of critical OLQs with limitations (for display) */
    val criticalWeaknessNames: List<String>,
    
    /** Whether Factor II average triggers auto-reject */
    val factorIIAutoReject: Boolean,
    
    /** Whether there are factor consistency issues */
    val hasFactorInconsistency: Boolean,
    
    /** Human-readable summary for expanded view */
    val detailedSummary: String
) {
    companion object {
        /**
         * Create UI model from validation result.
         * 
         * This is the single source of truth for transforming domain validation
         * into UI-ready display values.
         */
        fun fromValidationResult(
            result: OLQScoreValidationResult,
            entryType: EntryType
        ): SSBRecommendationUIModel {
            val recommendationText = when (result.recommendation) {
                RecommendationOutcome.RECOMMENDED -> "RECOMMENDED"
                RecommendationOutcome.BORDERLINE -> "BORDERLINE"
                RecommendationOutcome.NOT_RECOMMENDED -> "NOT RECOMMENDED"
            }
            
            val subtitleText = when (result.recommendation) {
                RecommendationOutcome.RECOMMENDED -> 
                    "You meet the SSB selection criteria"
                RecommendationOutcome.BORDERLINE -> 
                    "Performance is on the edge of passing"
                RecommendationOutcome.NOT_RECOMMENDED -> 
                    buildNotRecommendedSubtitle(result)
            }
            
            val maxLimitations = entryType.maxLimitations
            val limitationsOk = result.limitationCount <= maxLimitations
            
            val criticalWeaknessNames = result.criticalWeaknessOLQs.map { it.displayName }
            
            val detailedSummary = buildDetailedSummary(result, entryType)
            
            return SSBRecommendationUIModel(
                recommendation = result.recommendation,
                recommendationText = recommendationText,
                subtitleText = subtitleText,
                limitationCount = result.limitationCount,
                maxLimitations = maxLimitations,
                limitationsOk = limitationsOk,
                hasCriticalWeakness = result.hasCriticalWeakness,
                criticalWeaknessNames = criticalWeaknessNames,
                factorIIAutoReject = result.factorIIAutoReject,
                hasFactorInconsistency = result.hasFactorInconsistency,
                detailedSummary = detailedSummary
            )
        }
        
        private fun buildNotRecommendedSubtitle(result: OLQScoreValidationResult): String {
            return when {
                result.factorIIAutoReject -> 
                    "Factor II (Social Adjustment) auto-reject triggered"
                result.exceedsMaxLimitations -> 
                    "Too many limitations detected"
                result.hasCriticalWeakness -> 
                    "Critical OLQ weakness found"
                result.hasFactorInconsistency -> 
                    "Factor score inconsistency detected"
                else -> 
                    "Does not meet SSB selection criteria"
            }
        }
        
        private fun buildDetailedSummary(
            result: OLQScoreValidationResult,
            entryType: EntryType
        ): String {
            val parts = mutableListOf<String>()
            
            // Limitations summary
            parts.add("Limitations: ${result.limitationCount}/${entryType.maxLimitations}")
            
            // Critical weakness
            if (result.hasCriticalWeakness) {
                val names = result.criticalWeaknessOLQs.take(2).joinToString(", ") { it.displayName }
                parts.add("Critical: $names")
            } else {
                parts.add("Critical: OK")
            }
            
            // Factor II
            if (result.factorIIAutoReject) {
                parts.add("Factor II: ALERT")
            } else {
                parts.add("Factor II: OK")
            }
            
            return parts.joinToString(" | ")
        }
        
        /**
         * Create an empty/loading state model.
         */
        fun empty(): SSBRecommendationUIModel {
            return SSBRecommendationUIModel(
                recommendation = RecommendationOutcome.NOT_RECOMMENDED,
                recommendationText = "Analyzing...",
                subtitleText = "Validating scores against SSB criteria",
                limitationCount = 0,
                maxLimitations = 4,
                limitationsOk = true,
                hasCriticalWeakness = false,
                criticalWeaknessNames = emptyList(),
                factorIIAutoReject = false,
                hasFactorInconsistency = false,
                detailedSummary = "Loading..."
            )
        }
    }
}
