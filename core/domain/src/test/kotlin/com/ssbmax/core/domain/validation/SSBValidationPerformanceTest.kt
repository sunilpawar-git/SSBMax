package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.scoring.EntryType
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Performance tests for SSB Validation system.
 * 
 * Requirements:
 * - Single validation: <10ms
 * - Batch validation (100 candidates): <500ms
 * - Prompt generation: <50ms
 * 
 * These tests ensure the validation system is fast enough
 * for real-time use in workers without blocking.
 */
class SSBValidationPerformanceTest {

    // =========================================
    // SINGLE VALIDATION PERFORMANCE
    // =========================================

    @Test
    fun `single validation completes under 10ms`() {
        val scores = createValidScores()
        
        // Warm up JIT
        repeat(10) {
            SSBScoreValidator.validate(scores, EntryType.NDA)
        }
        
        // Measure actual performance
        val times = mutableListOf<Long>()
        repeat(100) {
            val nanos = measureNanoTime {
                SSBScoreValidator.validate(scores, EntryType.NDA)
            }
            times.add(nanos)
        }
        
        val avgMs = times.average() / 1_000_000
        val maxMs = times.maxOrNull()!! / 1_000_000
        
        println("Single validation: avg=${avgMs}ms, max=${maxMs}ms")
        
        assertTrue(
            "Average validation time should be under 10ms, was ${avgMs}ms",
            avgMs < 10.0
        )
        assertTrue(
            "Max validation time should be under 20ms, was ${maxMs}ms",
            maxMs < 20.0
        )
    }

    @Test
    fun `ValidationIntegration validateScores completes under 10ms`() {
        val scores = createOLQScoreMap()
        
        // Warm up
        repeat(10) {
            ValidationIntegration.validateScores(scores, EntryType.NDA)
        }
        
        // Measure
        val times = mutableListOf<Long>()
        repeat(100) {
            val nanos = measureNanoTime {
                ValidationIntegration.validateScores(scores, EntryType.NDA)
            }
            times.add(nanos)
        }
        
        val avgMs = times.average() / 1_000_000
        println("ValidationIntegration.validateScores: avg=${avgMs}ms")
        
        assertTrue(
            "ValidationIntegration should complete under 10ms, was ${avgMs}ms",
            avgMs < 10.0
        )
    }

    // =========================================
    // BATCH VALIDATION PERFORMANCE
    // =========================================

    @Test
    fun `batch validation of 100 candidates completes under 500ms`() {
        val candidates = (1..100).map { createRandomScores() }
        
        // Warm up
        candidates.take(10).forEach { 
            SSBScoreValidator.validate(it, EntryType.NDA) 
        }
        
        // Measure batch
        val nanos = measureNanoTime {
            candidates.forEach { scores ->
                SSBScoreValidator.validate(scores, EntryType.NDA)
            }
        }
        
        val totalMs = nanos / 1_000_000
        println("Batch validation (100 candidates): ${totalMs}ms")
        
        assertTrue(
            "Batch validation should complete under 500ms, was ${totalMs}ms",
            totalMs < 500
        )
    }

    // =========================================
    // INDIVIDUAL OPERATION PERFORMANCE
    // =========================================

    @Test
    fun `countLimitations is fast`() {
        val scores = createValidScores()
        
        val times = mutableListOf<Long>()
        repeat(1000) {
            val nanos = measureNanoTime {
                SSBScoreValidator.countLimitations(scores)
            }
            times.add(nanos)
        }
        
        val avgMicros = times.average() / 1000
        println("countLimitations: avg=${avgMicros}μs")
        
        assertTrue("countLimitations should be under 1ms", avgMicros < 1000)
    }

    @Test
    fun `checkFactorConsistency is fast`() {
        val scores = createValidScores()
        
        val times = mutableListOf<Long>()
        repeat(1000) {
            val nanos = measureNanoTime {
                SSBScoreValidator.checkFactorConsistency(scores)
            }
            times.add(nanos)
        }
        
        val avgMicros = times.average() / 1000
        println("checkFactorConsistency: avg=${avgMicros}μs")
        
        assertTrue("checkFactorConsistency should be under 1ms", avgMicros < 1000)
    }

    @Test
    fun `detectCriticalWeaknesses is fast`() {
        val scores = createValidScores()
        
        val times = mutableListOf<Long>()
        repeat(1000) {
            val nanos = measureNanoTime {
                SSBScoreValidator.detectCriticalWeaknesses(scores)
            }
            times.add(nanos)
        }
        
        val avgMicros = times.average() / 1000
        println("detectCriticalWeaknesses: avg=${avgMicros}μs")
        
        assertTrue("detectCriticalWeaknesses should be under 1ms", avgMicros < 1000)
    }

    @Test
    fun `calculateFactorAverages is fast`() {
        val scores = createValidScores()
        
        val times = mutableListOf<Long>()
        repeat(1000) {
            val nanos = measureNanoTime {
                SSBScoreValidator.calculateFactorAverages(scores)
            }
            times.add(nanos)
        }
        
        val avgMicros = times.average() / 1000
        println("calculateFactorAverages: avg=${avgMicros}μs")
        
        assertTrue("calculateFactorAverages should be under 1ms", avgMicros < 1000)
    }

    @Test
    fun `determineRecommendation is fast`() {
        val scores = createValidScores()
        
        val times = mutableListOf<Long>()
        repeat(1000) {
            val nanos = measureNanoTime {
                SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
            }
            times.add(nanos)
        }
        
        val avgMicros = times.average() / 1000
        println("determineRecommendation: avg=${avgMicros}μs")
        
        assertTrue("determineRecommendation should be under 2ms", avgMicros < 2000)
    }

    // =========================================
    // HELPERS
    // =========================================

    private fun createValidScores(): Map<OLQ, Int> {
        return OLQ.entries.associateWith { 5 } // All good scores
    }

    private fun createOLQScoreMap(): Map<OLQ, OLQScore> {
        return OLQ.entries.associateWith { olq ->
            OLQScore(
                score = 5,
                confidence = 80,
                reasoning = "Test score for ${olq.displayName}"
            )
        }
    }

    private fun createRandomScores(): Map<OLQ, Int> {
        return OLQ.entries.associateWith { (1..10).random() }
    }
}
