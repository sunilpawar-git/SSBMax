package com.ssbmax.core.data.local.entity

import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

class CachedOIRQuestionEntityTest {

    @get:Rule
    val timeout: Timeout = Timeout(60, TimeUnit.SECONDS)


    @Test
    fun entityInstance_canBeCreated() {
        val entity = CachedOIRQuestionEntity(
            id = "test-1",
            questionNumber = 1,
            type = "VERBAL_REASONING",
            subtype = "SYNONYMS",
            questionText = "What is the meaning of 'eloquent'?",
            optionsJson = """["A","B","C","D"]""",
            correctAnswerId = "A",
            explanation = "Eloquent means fluent or persuasive in speaking or writing.",
            questionImageUrl = null,
            difficulty = "EASY",
            tags = "vocabulary",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0
        )

        assert(entity.id == "test-1")
        assert(entity.questionNumber == 1)
        assert(entity.type == "VERBAL_REASONING")
        assert(entity.difficulty == "EASY")
    }

    @Test
    fun entityHasNullableFields() {
        val entity = CachedOIRQuestionEntity(
            id = "test-1",
            questionNumber = 1,
            type = "VERBAL_REASONING",
            subtype = null,
            questionText = "Test?",
            optionsJson = """["A"]""",
            correctAnswerId = "A",
            explanation = "Test.",
            questionImageUrl = null,
            difficulty = "EASY",
            tags = "test",
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = 0
        )

        assert(entity.subtype == null)
        assert(entity.questionImageUrl == null)
        assert(entity.lastUsed == null)
    }

    @Test
    fun correctAnswerIds_defaultsToNull_forSingleAnswerQuestion() {
        // Single-answer questions don't set correctAnswerIds; null means "use correctAnswerId"
        val entity = CachedOIRQuestionEntity(
            id = "single-1",
            questionNumber = 5,
            type = "NON_VERBAL_REASONING",
            subtype = null,
            questionText = "Which figure comes next?",
            optionsJson = """["A","B","C","D","E"]""",
            correctAnswerId = "opt_c",
            explanation = "Pattern continues with figure C.",
            questionImageUrl = "https://example.com/fig.png",
            difficulty = "MEDIUM",
            tags = "figure",
            batchId = "batch_001",
            cachedAt = 1000L,
            lastUsed = null,
            usageCount = 0
        )

        assert(entity.correctAnswerIds == null) {
            "Single-answer entity should have correctAnswerIds = null by default"
        }
    }

    @Test
    fun correctAnswerIds_storesJsonForMultiAnswerQuestion() {
        val json = """["opt_b","opt_c"]"""
        val entity = CachedOIRQuestionEntity(
            id = "multi-1",
            questionNumber = 10,
            type = "NON_VERBAL_REASONING",
            subtype = null,
            questionText = "Which two figures belong to Class A?",
            optionsJson = """["A","B","C","D","E"]""",
            correctAnswerId = "",
            explanation = "Answer: 2 and 3.",
            questionImageUrl = "https://example.com/fig2.png",
            difficulty = "HARD",
            tags = "figure,multi-select",
            batchId = "batch_001",
            cachedAt = 2000L,
            lastUsed = null,
            usageCount = 0,
            correctAnswerIds = json
        )

        assert(entity.correctAnswerIds == json) {
            "Multi-answer entity should store the JSON list of option IDs"
        }
    }
}
