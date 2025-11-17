package com.ssbmax.core.data.util

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.Executor

/**
 * Utility functions for testing Firebase Tasks with MockK
 */

/**
 * Creates a successful Task mock that returns the given result
 */
fun <T> successTask(result: T): Task<T> {
    val task: Task<T> = mockk(relaxed = true)
    every { task.isSuccessful } returns true
    every { task.result } returns result
    every { task.isCanceled } returns false
    every { task.isComplete } returns true
    every { task.exception } returns null

    val successSlot = slot<OnSuccessListener<in T>>()
    every { task.addOnSuccessListener(capture(successSlot)) } answers {
        successSlot.captured.onSuccess(result)
        task
    }

    every { task.addOnSuccessListener(any<Executor>(), capture(successSlot)) } answers {
        successSlot.captured.onSuccess(result)
        task
    }

    every { task.addOnFailureListener(any()) } returns task
    every { task.addOnFailureListener(any<Executor>(), any()) } returns task

    return task
}

/**
 * Creates a failed Task mock that throws the given exception
 */
fun <T> failureTask(exception: Exception): Task<T> {
    val task: Task<T> = mockk(relaxed = true)
    every { task.isSuccessful } returns false
    every { task.isCanceled } returns false
    every { task.isComplete } returns true
    every { task.exception } returns exception

    val failureSlot = slot<OnFailureListener>()
    every { task.addOnFailureListener(capture(failureSlot)) } answers {
        failureSlot.captured.onFailure(exception)
        task
    }

    every { task.addOnFailureListener(any<Executor>(), capture(failureSlot)) } answers {
        failureSlot.captured.onFailure(exception)
        task
    }

    every { task.addOnSuccessListener(any()) } returns task
    every { task.addOnSuccessListener(any<Executor>(), any<OnSuccessListener<in T>>()) } returns task

    return task
}

/**
 * Extension to make Tasks work with kotlinx-coroutines-play-services' await()
 */
suspend fun <T> Task<T>.await(): T {
    if (isSuccessful) {
        return result
    } else {
        throw exception ?: Exception("Task failed without exception")
    }
}
