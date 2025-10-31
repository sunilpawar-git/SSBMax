package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking downloaded OIR question batches
 * 
 * Helps manage cache and know which batches are available locally
 */
@Entity(tableName = "oir_batch_metadata")
data class OIRBatchMetadataEntity(
    @PrimaryKey 
    val batchId: String, // batch_001, batch_002, etc.
    
    val downloadedAt: Long, // When this batch was downloaded
    
    val questionCount: Int, // Number of questions in this batch
    
    val version: String // Version of the batch (for updates)
)

