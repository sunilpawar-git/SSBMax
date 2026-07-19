package com.ssbmax.core.data.local

import com.google.gson.Gson
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.shared.domain.model.DeviationTolerance
import com.ssbmax.shared.domain.model.GenderTag
import com.ssbmax.shared.domain.model.PPDTImageContext
import org.junit.Assert.assertEquals
import org.junit.Test

class PPDTImageContextMappingTest {

    private val gson = Gson()

    @Test
    fun `PPDTImageContext serializes to and from JSON without data loss`() {
        // WHY: Room stores imageContext as a JSON string — a lossy roundtrip would silently
        // corrupt Phase 8 prompt data (core elements, OLQ lists, penalty themes)
        val original = PPDTImageContext(
            sceneDescription = "Three officers planning a river crossing",
            coreElements = listOf("map on table", "officers in uniform", "river in background"),
            ambiguousElements = listOf("civilian present", "time of day"),
            expectedThemes = listOf("leadership", "strategy", "coordination"),
            penalizedThemes = listOf("unrelated personal drama"),
            primaryOLQs = listOf("ORGANIZING_ABILITY", "INFLUENCE_GROUP"),
            deviationTolerance = DeviationTolerance.MEDIUM,
            exemplarGoodHints = listOf("Hero takes charge of the plan"),
            exemplarBadHints = listOf("Story about a sports match")
        )
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, PPDTImageContext::class.java)
        assertEquals(original, restored)
    }

    @Test
    fun `CachedPPDTImageEntity defaults genderTag to MIXED for backward compat`() {
        // WHY: Old cached rows from before Phase 6 have no genderTag; defaulting to MIXED ensures
        // they are shown to ALL users (no one gets blocked by an accidental gender mismatch)
        val entity = CachedPPDTImageEntity(
            id = "test_id",
            imageUrl = "https://example.com/image.jpg",
            imageDescription = "Test scene",
            imageContextJson = "{}",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis()
        )
        assertEquals(GenderTag.MIXED, entity.genderTag)
    }
}
