package com.ssbmax.core.data.repository

import android.util.Log

/**
 * Centralized error handling for repository operations.
 * Standardizes Result<T> error handling and logging patterns across all repositories.
 */
object RepositoryErrorHandler {

    private const val DEFAULT_TAG = "RepositoryError"

    /**
     * Execute a repository operation with standardized error handling.
     *
     * @param tag Log tag for this operation (typically repository class name)
     * @param operationName Descriptive name for logging (e.g., "load user profile")
     * @param operation The suspend operation to execute
     * @return Result<T> with success or failure
     */
    suspend fun <T> execute(
        tag: String = DEFAULT_TAG,
        operationName: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Log.d(tag, "$operationName succeeded")
            Result.success(result)
        } catch (e: Exception) {
            Log.w(tag, "$operationName failed", e)
            Result.failure(e)
        }
    }

    /**
     * Execute a repository operation with custom error message.
     *
     * @param tag Log tag for this operation
     * @param operationName Descriptive name for logging
     * @param errorMessage Custom error message to wrap the exception
     * @param operation The suspend operation to execute
     * @return Result<T> with success or failure (failure includes custom message)
     */
    suspend fun <T> executeWithMessage(
        tag: String = DEFAULT_TAG,
        operationName: String,
        errorMessage: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Log.d(tag, "$operationName succeeded")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "$operationName failed: $errorMessage", e)
            Result.failure(Exception("$errorMessage: ${e.message}", e))
        }
    }

    /**
     * Execute a repository operation with default value on failure.
     * Useful for operations where failure should return a default/empty state.
     *
     * @param tag Log tag for this operation
     * @param operationName Descriptive name for logging
     * @param defaultValue Value to return on failure
     * @param operation The suspend operation to execute
     * @return Result<T> with success or default value on failure
     */
    suspend fun <T> executeWithDefault(
        tag: String = DEFAULT_TAG,
        operationName: String,
        defaultValue: T,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Log.d(tag, "$operationName succeeded")
            Result.success(result)
        } catch (e: Exception) {
            Log.w(tag, "$operationName failed, using default value", e)
            Result.success(defaultValue)
        }
    }
}
