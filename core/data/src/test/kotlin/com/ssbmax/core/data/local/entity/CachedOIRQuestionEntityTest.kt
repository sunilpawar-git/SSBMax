package com.ssbmax.core.data.local.entity

import org.junit.Test

class CachedOIRQuestionEntityTest {

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
}
