package com.ssbmax.shared.ui.oir

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.DifficultyScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.QuestionDifficulty

internal fun visibleOIRCategoryScores(
    scores: Map<OIRQuestionType, CategoryScore>
): List<CategoryScore> = scores.values.filter { it.totalQuestions > 0 }

internal fun orderedOIRDifficultyScores(
    scores: Map<QuestionDifficulty, DifficultyScore>
): List<DifficultyScore> = QuestionDifficulty.values().mapNotNull(scores::get)

internal fun visibleOIRDifficultyScores(
    scores: Map<QuestionDifficulty, DifficultyScore>
): List<DifficultyScore> = orderedOIRDifficultyScores(scores)
    .filter { it.totalQuestions > 0 }

internal fun hasMeaningfulOIRDifficultyBreakdown(
    scores: Map<QuestionDifficulty, DifficultyScore>
): Boolean = visibleOIRDifficultyScores(scores).size > 1
