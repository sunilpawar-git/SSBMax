package com.ssbmax.shared.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific SQLDelight driver creation — the actual Room -> SQLDelight
 * migration point flagged in the KMP plan's Phase 0 exit report as unexercised.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
