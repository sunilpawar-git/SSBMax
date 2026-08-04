package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.StudyProgress
import com.ssbmax.shared.domain.model.StudySession
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trips StudyProgress/StudySession through the Firestore wire DTOs.
 * Why it matters: this is the seam GitLiveStudyProgressRepository depends on
 * instead of the Android SDK's hand-rolled DocumentSnapshot extension
 * functions in StudyProgressRepositoryImpl — a silent field-name mismatch
 * here would corrupt reading-progress/bookmark data without the build failing.
 */
class StudyProgressDtoTest {

    @Test
    fun `study progress round-trips every field including highlights and notes`() {
        val progress = StudyProgress(
            materialId = "material-1",
            userId = "user-1",
            progress = 42.5f,
            lastReadAt = 1_700_000_000_000L,
            timeSpent = 60_000L,
            isBookmarked = true,
            isCompleted = false,
            notes = "revisit section 3",
            highlights = listOf("h1", "h2")
        )
        assertEquals(progress, progress.toDto().toDomain())
    }

    @Test
    fun `study progress with null optional fields round-trips as null`() {
        val progress = StudyProgress(materialId = "m", userId = "u")
        assertEquals(progress, progress.toDto().toDomain())
    }

    @Test
    fun `study session round-trips including active sessions with no end time`() {
        val session = StudySession(
            id = "session-1",
            userId = "user-1",
            materialId = "material-1",
            startedAt = 1_700_000_000_000L,
            endedAt = null,
            duration = 0L,
            progressIncrement = 0f
        )
        assertEquals(session, session.toDto().toDomain(session.id))
    }

    @Test
    fun `completed study session preserves endedAt and duration`() {
        val session = StudySession(
            id = "session-2",
            userId = "user-1",
            materialId = "material-1",
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            duration = 600_000L,
            progressIncrement = 5f
        )
        val roundTripped = session.toDto().toDomain(session.id)
        assertEquals(session, roundTripped)
        assertEquals(false, roundTripped.isActive)
    }
}
