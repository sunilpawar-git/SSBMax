package com.ssbmax.core.domain.scoring

/**
 * Entry type for SSB candidates - determines limitation thresholds
 *
 * Different entry types have different maximum allowed limitations:
 * - NDA: Strict (max 4 limitations) - younger candidates, more trainable
 * - OTA: Lenient (max 7 limitations) - short service commission
 * - GRADUATE: Lenient (max 7 limitations) - direct entry graduates
 */
enum class EntryType(val maxLimitations: Int) {
    /**
     * National Defence Academy entry
     * Most stringent - candidates are young (16.5-19.5 years)
     * Maximum 4 limitations allowed
     */
    NDA(4),
    
    /**
     * Officers Training Academy entry
     * Short Service Commission route
     * Maximum 7 limitations allowed
     */
    OTA(7),
    
    /**
     * Graduate entry (CDS, TGC, etc.)
     * Direct entry for graduates
     * Maximum 7 limitations allowed
     */
    GRADUATE(7)
}
