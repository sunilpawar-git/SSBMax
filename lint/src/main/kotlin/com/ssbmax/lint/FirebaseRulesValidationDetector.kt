package com.ssbmax.lint

import com.android.tools.lint.detector.api.*
import java.util.EnumSet

/**
 * Custom Lint Detector - Firebase Rules Validation (Placeholder)
 *
 * This detector is a placeholder for Phase 3 implementation.
 * In a full implementation, it would validate firestore.rules file syntax at build time.
 *
 * For now, validation is done via pre-commit hooks:
 *   firebase deploy --only firestore:rules --dry-run
 *
 * Future implementation will check for:
 * - Mismatched braces (unclosed rules)
 * - Missing semicolons (allow/deny statements)
 * - Basic structural issues
 *
 * See: docs/security/SECURITY.md, Phase 3 section
 */
class FirebaseRulesValidationDetector : Detector() {

    companion object {
        private const val ISSUE_ID = "FirebaseRulesValidation"
        private const val ISSUE_DESCRIPTION = "Firebase rules syntax warning (Phase 3 TODO)"
        private const val ISSUE_EXPLANATION = """
            Firebase rules validation is handled via pre-commit hooks in Phase 3.
            
            For now, validate manually:
                firebase deploy --only firestore:rules --dry-run
            
            See: docs/security/SECURITY.md for full details.
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(
                FirebaseRulesValidationDetector::class.java,
                EnumSet.of(Scope.OTHER)
            )
        )
    }
}
