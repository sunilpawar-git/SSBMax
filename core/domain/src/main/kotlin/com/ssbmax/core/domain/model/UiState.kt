package com.ssbmax.core.domain.model

/**
 * Sealed interface representing UI state for ViewModels.
 * Provides type-safe state management with explicit loading, success, error, and empty states.
 * 
 * Usage:
 * ```kotlin
 * sealed class MyScreenState : UiState<MyData> {
 *     object Loading : MyScreenState(), UiState.Loading
 *     data class Success(override val data: MyData) : MyScreenState(), UiState.Success<MyData>
 *     data class Error(override val message: String) : MyScreenState(), UiState.Error
 *     object Empty : MyScreenState(), UiState.Empty
 * }
 * ```
 * 
 * Or use directly:
 * ```kotlin
 * sealed interface MyScreenState {
 *     data object Loading : MyScreenState
 *     data class Success(val data: MyData) : MyScreenState
 *     data class Error(val message: String) : MyScreenState
 *     data object Empty : MyScreenState
 * }
 * ```
 */
sealed interface UiState<out T> {
    
    /**
     * Loading state - operation in progress
     */
    interface Loading : UiState<Nothing> {
        /**
         * Optional loading message to display to user
         */
        val message: String? get() = null
    }
    
    /**
     * Success state - operation completed successfully with data
     */
    interface Success<out T> : UiState<T> {
        val data: T
    }
    
    /**
     * Error state - operation failed with error message
     */
    interface Error : UiState<Nothing> {
        val message: String
        val throwable: Throwable? get() = null
    }
    
    /**
     * Empty state - no data available (e.g., empty list, no content)
     * Use this instead of Success with null/empty data for better semantics
     */
    interface Empty : UiState<Nothing> {
        val message: String? get() = null
    }
}

/**
 * Default implementations for common use cases.
 * Can be used directly or extended with custom properties.
 */
object UiStateDefaults {
    
    data object Loading : UiState.Loading
    
    data class LoadingWithMessage(override val message: String) : UiState.Loading
    
    data class Success<T>(override val data: T) : UiState.Success<T>
    
    data class Error(
        override val message: String,
        override val throwable: Throwable? = null
    ) : UiState.Error
    
    data object Empty : UiState.Empty
    
    data class EmptyWithMessage(override val message: String) : UiState.Empty
}

/**
 * Extension functions for working with UiState
 */

/**
 * Returns the data if state is Success, null otherwise
 */
fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

/**
 * Returns true if state is Loading
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading

/**
 * Returns true if state is Success
 */
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success

/**
 * Returns true if state is Error
 */
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error

/**
 * Returns true if state is Empty
 */
fun <T> UiState<T>.isEmpty(): Boolean = this is UiState.Empty

/**
 * Maps the data of a Success state, preserving other states
 */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Success -> UiStateDefaults.Success(transform(data))
    is UiState.Loading -> this
    is UiState.Error -> this
    is UiState.Empty -> this
}

/**
 * Executes action if state is Success
 */
inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) {
        action(data)
    }
    return this
}

/**
 * Executes action if state is Error
 */
inline fun <T> UiState<T>.onError(action: (String, Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) {
        action(message, throwable)
    }
    return this
}

/**
 * Executes action if state is Loading
 */
inline fun <T> UiState<T>.onLoading(action: (String?) -> Unit): UiState<T> {
    if (this is UiState.Loading) {
        action(message)
    }
    return this
}

/**
 * Executes action if state is Empty
 */
inline fun <T> UiState<T>.onEmpty(action: (String?) -> Unit): UiState<T> {
    if (this is UiState.Empty) {
        action(message)
    }
    return this
}

