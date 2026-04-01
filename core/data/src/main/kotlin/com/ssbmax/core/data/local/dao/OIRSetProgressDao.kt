package com.ssbmax.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssbmax.core.data.local.entity.OIRSetProgressEntity

/**
 * Data Access Object for OIR practice set progress tracking.
 * Supports sequential unlock: a set is unlocked only after the previous one is completed.
 */
@Dao
interface OIRSetProgressDao {

    @Query("SELECT * FROM oir_set_progress WHERE userId = :userId ORDER BY setNumber ASC")
    suspend fun getProgressByUserId(userId: String): List<OIRSetProgressEntity>

    @Query("SELECT * FROM oir_set_progress WHERE id = :id")
    suspend fun getProgressById(id: String): OIRSetProgressEntity?

    @Upsert
    suspend fun upsertProgress(entity: OIRSetProgressEntity)

    @Query("DELETE FROM oir_set_progress WHERE userId = :userId")
    suspend fun deleteProgressByUserId(userId: String)
}
