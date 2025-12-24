package com.ssbmax.core.domain.util

/**
 * Platform-independent logging interface for the domain layer
 * 
 * The domain layer must remain Android-independent for:
 * - Platform portability (KMM, JVM, etc.)
 * - Unit testing without Android dependencies
 * - Clean architecture principles
 * 
 * Implementation is provided by the data/presentation layers via dependency injection.
 */
interface DomainLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
    fun v(tag: String, message: String)
}

/**
 * No-op logger for testing or when logging is disabled
 */
class NoOpLogger : DomainLogger {
    override fun d(tag: String, message: String) {}
    override fun e(tag: String, message: String, throwable: Throwable?) {}
    override fun w(tag: String, message: String) {}
    override fun i(tag: String, message: String) {}
    override fun v(tag: String, message: String) {}
}

