package com.ssbmax.shared.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Clock

/**
 * Shared per-tick countdown loop, extracted from
 * [com.ssbmax.shared.presentation.tat.TATTestViewModel]'s two timer functions and
 * [com.ssbmax.shared.presentation.srt.SRTTestViewModel]'s one timer function to keep all three
 * files under the 300-line limit (same "extract to a sibling file" precedent as
 * [com.ssbmax.shared.presentation.piqresult.PIQSubmissionParser]). Covers only the duplicated
 * while-loop body -- callers still own starting/cancelling the job, computing [endTimeMillis],
 * and resetting `isTimerActive` in their own try/finally, since that state lives on each
 * ViewModel's own `_uiState`/`timerStartTime` generation counter.
 */
internal suspend fun CoroutineScope.runCountdownLoop(
    endTimeMillis: Long,
    tickMillis: Long = 200L,
    onTick: (secondsRemaining: Int) -> Unit
) {
    while (isActive) {
        val remaining = ((endTimeMillis - Clock.System.now().toEpochMilliseconds()) / 1000).toInt()
        if (remaining <= 0) break
        onTick(remaining)
        delay(tickMillis)
    }
}
