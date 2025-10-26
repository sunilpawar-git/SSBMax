package com.ssbmax.core.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UserProfile domain model
 * Tests validation logic, initialization, and utility methods
 */
class UserProfileTest {
    
    @Test
    fun `valid UserProfile is created successfully`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        // Then
        assertEquals("user123", profile.userId)
        assertEquals("John Doe", profile.fullName)
        assertEquals(25, profile.age)
        assertEquals(Gender.MALE, profile.gender)
        assertEquals(EntryType.GRADUATE, profile.entryType)
        assertEquals(SubscriptionType.FREE, profile.subscriptionType)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `creating UserProfile with blank name throws exception`() {
        // When
        UserProfile(
            userId = "user123",
            fullName = "   ",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        // Then exception is thrown
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `creating UserProfile with age below 18 throws exception`() {
        // When
        UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 17,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        // Then exception is thrown
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `creating UserProfile with age above 35 throws exception`() {
        // When
        UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 36,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        // Then exception is thrown
    }
    
    @Test
    fun `getInitials returns correct initials for full name`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        // When
        val initials = profile.getInitials()
        
        // Then
        assertEquals("JD", initials)
    }
    
    @Test
    fun `getInitials returns first two letters for single name`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "John",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        // When
        val initials = profile.getInitials()
        
        // Then
        assertEquals("JO", initials)
    }
    
    @Test
    fun `getInitials handles multiple names correctly`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "John Michael Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        // When
        val initials = profile.getInitials()
        
        // Then - should use first and last name
        assertEquals("JD", initials)
    }
    
    @Test
    fun `getInitials handles names with extra spaces`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "  John   Doe  ",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        // When
        val initials = profile.getInitials()
        
        // Then
        assertEquals("JD", initials)
    }
    
    @Test
    fun `Gender fromDisplayName returns correct enum`() {
        assertEquals(Gender.MALE, Gender.fromDisplayName("Male"))
        assertEquals(Gender.FEMALE, Gender.fromDisplayName("Female"))
        assertEquals(Gender.OTHER, Gender.fromDisplayName("Other"))
    }
    
    @Test
    fun `Gender fromDisplayName is case insensitive`() {
        assertEquals(Gender.MALE, Gender.fromDisplayName("male"))
        assertEquals(Gender.FEMALE, Gender.fromDisplayName("FEMALE"))
        assertEquals(Gender.OTHER, Gender.fromDisplayName("oThEr"))
    }
    
    @Test
    fun `Gender fromDisplayName returns null for invalid name`() {
        assertNull(Gender.fromDisplayName("Invalid"))
        assertNull(Gender.fromDisplayName(""))
    }
    
    @Test
    fun `EntryType fromDisplayName returns correct enum`() {
        assertEquals(EntryType.ENTRY_10_PLUS_2, EntryType.fromDisplayName("10+2 Entry"))
        assertEquals(EntryType.GRADUATE, EntryType.fromDisplayName("Graduate Entry"))
        assertEquals(EntryType.SERVICE, EntryType.fromDisplayName("Service Entry"))
    }
    
    @Test
    fun `EntryType fromDisplayName is case insensitive`() {
        assertEquals(EntryType.GRADUATE, EntryType.fromDisplayName("graduate entry"))
        assertEquals(EntryType.SERVICE, EntryType.fromDisplayName("SERVICE ENTRY"))
    }
    
    @Test
    fun `EntryType fromDisplayName returns null for invalid name`() {
        assertNull(EntryType.fromDisplayName("Invalid Entry"))
        assertNull(EntryType.fromDisplayName(""))
    }
    
    @Test
    fun `UserProfile with premium subscription is created correctly`() {
        // Given
        val profile = UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE,
            subscriptionType = SubscriptionType.PREMIUM_AI
        )
        
        // Then
        assertEquals(SubscriptionType.PREMIUM_AI, profile.subscriptionType)
    }
    
    @Test
    fun `UserProfile timestamps are set correctly`() {
        // Given
        val beforeCreation = System.currentTimeMillis()
        
        val profile = UserProfile(
            userId = "user123",
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE,
            entryType = EntryType.GRADUATE
        )
        
        val afterCreation = System.currentTimeMillis()
        
        // Then - timestamps should be within the test execution window
        assertTrue(profile.createdAt >= beforeCreation)
        assertTrue(profile.createdAt <= afterCreation)
        assertTrue(profile.updatedAt >= beforeCreation)
        assertTrue(profile.updatedAt <= afterCreation)
    }
}

