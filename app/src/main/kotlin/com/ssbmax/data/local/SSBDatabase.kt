package com.ssbmax.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssbmax.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.data.local.entity.TATStoryAssessmentEntity

/**
 * SSBMax Room database. KMP-convergence Phase 9f moved this out of the
 * (now-deleted) `core:data` module — every other repository/cache this
 * database used to back (OIR/WAT/SRT/TAT-image/GTO/GPE/interview caches,
 * subscription usage mirror, user-performance adaptive-difficulty tracking)
 * was ported to `shared`'s GitLive/SQLDelight repositories in Phases 9a-9d
 * or found dead by Phase 9f's own binding audit. The one genuinely live
 * survivor is [TATStoryAssessmentDao] — a local synthesis buffer between
 * [com.ssbmax.workers.TATStoryAnalysisWorker] and
 * [com.ssbmax.workers.TATSynthesisWorker], both Android `WorkManager` jobs
 * with no iOS equivalent (iOS's single-coroutine
 * `com.ssbmax.shared.analysis.TATAnalysisOrchestrator` holds the same data
 * in memory instead) — Android-only platform glue belongs in `app`, not
 * `shared`'s cross-platform SQLDelight database.
 */
@Database(
    entities = [
        TATStoryAssessmentEntity::class
    ],
    // v30 (Phase 9f): dropped UserPerformanceEntity/UserPerformanceDao — the
    // only reader/writer, core:data's DifficultyProgressionManager, had zero
    // production callers (superseded by shared's
    // GitLiveDifficultyProgressionManager). No explicit migration: same
    // fallbackToDestructiveMigration()-only precedent as v27/v28/v29 — no
    // production users, re-downloadable/regenerable local data.
    version = 30,
    exportSchema = true
)
abstract class SSBDatabase : RoomDatabase() {

    /**
     * TAT per-story assessment cache DAO
     */
    abstract fun tatStoryAssessmentDao(): TATStoryAssessmentDao

    companion object {
        const val DATABASE_NAME = "ssbmax_database"
    }
}
