package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trips UserProfile through the Firestore wire DTO. Why it matters:
 * this is the one seam the GitLive typed-document read/write depends on
 * instead of the Android SDK's hand-rolled Map<String, Any?> mapping — a
 * silent enum-name mismatch here (e.g. Gender.OTHER not matching Firestore's
 * stored string) would corrupt profile data without the build ever failing.
 */
class UserProfileDtoTest {

    private val profile = UserProfile(
        userId = "user-1",
        fullName = "Jordan Rivera",
        age = 24,
        gender = Gender.OTHER,
        entryType = EntryType.SERVICE,
        profilePictureUrl = "https://example.com/pic.jpg",
        currentStreak = 5,
        lastLoginDate = 1_700_000_000_000L,
        longestStreak = 12,
        createdAt = 1_600_000_000_000L,
        updatedAt = 1_700_000_000_000L
    )

    @Test
    fun `domain to dto to domain round-trips every field`() {
        val roundTripped = profile.toDto().toDomain()
        assertEquals(profile, roundTripped)
    }

    @Test
    fun `unrecognized enum strings fall back to safe defaults instead of throwing`() {
        val dto = UserProfileDto(userId = "u", fullName = "X", age = 20, gender = "NOT_A_GENDER", entryType = "NOT_AN_ENTRY")
        val domain = dto.toDomain()
        assertEquals(Gender.MALE, domain.gender)
        assertEquals(EntryType.GRADUATE, domain.entryType)
    }

    @Test
    fun `isProfileComplete mirrors the Android impl's blank-name and positive-age check`() {
        // UserProfile's own init{} already requires a non-blank name and age in
        // 18..35, so any constructible instance is "complete" by this
        // definition -- this pins that (intentionally redundant) parity with
        // the Android impl rather than silently dropping the check on the KMP side.
        assertTrue(profile.isProfileComplete())
    }
}
