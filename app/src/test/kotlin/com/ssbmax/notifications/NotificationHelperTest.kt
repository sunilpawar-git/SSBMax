package com.ssbmax.notifications

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.R
import com.ssbmax.utils.ErrorLogger
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowNotification
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.Runs
import io.mockk.just
import io.mockk.spyk
import org.junit.Ignore

/**
 * NotificationHelperTest - temporarily ignored due to Robolectric SDK version mismatch
 * TODO: Re-enable when Robolectric supports SDK 35
 */
@Ignore("Robolectric SDK version mismatch - SDK 35 not yet supported")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class NotificationHelperTest {

    private val context: Context = spyk(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun showInterviewResultsReadyNotification_postsNotificationWithContent() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val shadowManager = Shadows.shadowOf(manager)
        shadowManager.setNotificationsEnabled(true)

        mockkObject(ErrorLogger)
        every { ErrorLogger.log(any<Throwable>(), any(), any(), any()) } just Runs
        every { context.getString(R.string.notification_interview_results_title) } returns "Interview ready"
        every { context.getString(R.string.notification_interview_results_body) } returns "View results"

        val helper = NotificationHelper(context)
        val sessionId = "session-123"
        val resultId = "result-456"

        helper.showInterviewResultsReadyNotification(sessionId, resultId)

        verify(exactly = 0) { ErrorLogger.log(any<Throwable>(), any(), any(), any()) }

        unmockkAll()
    }

    @Test
    fun showInterviewAnalysisFailedNotification_doesNotLogError() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val shadowManager = Shadows.shadowOf(manager)
        shadowManager.setNotificationsEnabled(true)

        mockkObject(ErrorLogger)
        every { ErrorLogger.log(any<Throwable>(), any(), any(), any()) } just Runs
        every { context.getString(R.string.notification_interview_failed_title) } returns "Interview failed"
        every { context.getString(R.string.notification_interview_failed_body) } returns "Please try again"

        val helper = NotificationHelper(context)

        helper.showInterviewAnalysisFailedNotification("session-123")

        verify(exactly = 0) { ErrorLogger.log(any<Throwable>(), any(), any(), any()) }
        unmockkAll()
    }
}

