package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the pure aggregation functions extracted from
 * [GitLiveUnifiedResultRepository] (mirrors the Android `UnifiedResultRepositoryImpl`'s inline
 * logic for combining OLQ scores across GTO/Interview results). Each assertion encodes a real
 * SSB-domain rule this repo depends on, not a trivial getter check.
 */
class UnifiedResultDtoTest {

    private fun score(value: Int) = OLQScore(score = value, confidence = 80, reasoning = "test")

    @Test
    fun gtoTestTypeToTestType_mapsAllEightGtoTypes() {
        assertEquals(TestType.GTO_GD, gtoTestTypeToTestType(GTOTestType.GROUP_DISCUSSION))
        assertEquals(TestType.GTO_GPE, gtoTestTypeToTestType(GTOTestType.GROUP_PLANNING_EXERCISE))
        assertEquals(TestType.GTO_LECTURETTE, gtoTestTypeToTestType(GTOTestType.LECTURETTE))
        assertEquals(TestType.GTO_PGT, gtoTestTypeToTestType(GTOTestType.PROGRESSIVE_GROUP_TASK))
        assertEquals(TestType.GTO_HGT, gtoTestTypeToTestType(GTOTestType.HALF_GROUP_TASK))
        assertEquals(TestType.GTO_GOR, gtoTestTypeToTestType(GTOTestType.GROUP_OBSTACLE_RACE))
        assertEquals(TestType.GTO_IO, gtoTestTypeToTestType(GTOTestType.INDIVIDUAL_OBSTACLES))
        assertEquals(TestType.GTO_CT, gtoTestTypeToTestType(GTOTestType.COMMAND_TASK))
    }

    @Test
    fun averageOlqScores_averagesAcrossMultipleResults() {
        // Same OLQ scored in two different result sources (e.g. one GTO test, one interview) —
        // the repo must average, not just take the last value, or the OLQ profile would be wrong.
        val maps = listOf(
            mapOf(OLQ.COURAGE to score(4)),
            mapOf(OLQ.COURAGE to score(6))
        )
        val averages = averageOlqScores(maps)
        assertEquals(5f, averages.getValue(OLQ.COURAGE))
    }

    @Test
    fun averageOlqScores_omitsOlqsNeverScored() {
        val averages = averageOlqScores(listOf(mapOf(OLQ.COURAGE to score(4))))
        assertEquals(setOf(OLQ.COURAGE), averages.keys)
    }

    @Test
    fun rankOlqsAscending_lowestScoreIsStrongest() {
        // SSB convention: LOWER score = stronger candidate, so "top strengths" must sort ascending.
        val averages = mapOf(OLQ.COURAGE to 8f, OLQ.INITIATIVE to 2f, OLQ.SELF_CONFIDENCE to 5f)
        val top2 = rankOlqsAscending(averages, topN = 2)
        assertEquals(listOf(OLQ.INITIATIVE, OLQ.SELF_CONFIDENCE), top2)
    }

    @Test
    fun rankOlqsDescending_highestScoreNeedsMostImprovement() {
        val averages = mapOf(OLQ.COURAGE to 8f, OLQ.INITIATIVE to 2f, OLQ.SELF_CONFIDENCE to 5f)
        val weakest2 = rankOlqsDescending(averages, topN = 2)
        assertEquals(listOf(OLQ.COURAGE, OLQ.SELF_CONFIDENCE), weakest2)
    }
}
