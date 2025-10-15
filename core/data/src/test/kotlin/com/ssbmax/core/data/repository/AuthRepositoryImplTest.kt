package com.ssbmax.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for AuthRepositoryImpl
 */
class AuthRepositoryImplTest {
    
    private lateinit var repository: AuthRepositoryImpl
    
    @Before
    fun setup() {
        repository = AuthRepositoryImpl()
    }
    
    @Test
    fun `signUp creates new user successfully`() = runTest {
        // When
        val result = repository.signUp("test@example.com", "password123", "Test User")
        
        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user?.email)
        assertEquals("Test User", user?.displayName)
        assertFalse(user?.isPremium ?: true)
    }
    
    @Test
    fun `signUp with existing email fails`() = runTest {
        // Given
        repository.signUp("test@example.com", "password123", "Test User")
        
        // When
        val result = repository.signUp("test@example.com", "newpassword", "Another User")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already registered") ?: false)
    }
    
    @Test
    fun `signIn with correct credentials succeeds`() = runTest {
        // Given
        repository.signUp("test@example.com", "password123", "Test User")
        
        // When
        val result = repository.signIn("test@example.com", "password123")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("test@example.com", result.getOrNull()?.email)
    }
    
    @Test
    fun `signIn with wrong password fails`() = runTest {
        // Given
        repository.signUp("test@example.com", "password123", "Test User")
        
        // When
        val result = repository.signIn("test@example.com", "wrongpassword")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid") ?: false)
    }
    
    @Test
    fun `signIn with non-existent email fails`() = runTest {
        // When
        val result = repository.signIn("nonexistent@example.com", "password123")
        
        // Then
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `signUp updates currentUser flow`() = runTest {
        // When
        repository.signUp("test@example.com", "password123", "Test User")
        
        // Then
        val currentUser = repository.currentUser.first()
        assertNotNull(currentUser)
        assertEquals("test@example.com", currentUser?.email)
    }
    
    @Test
    fun `signOut clears currentUser`() = runTest {
        // Given
        repository.signUp("test@example.com", "password123", "Test User")
        
        // When
        repository.signOut()
        
        // Then
        val currentUser = repository.currentUser.first()
        assertNull(currentUser)
    }
    
    @Test
    fun `isAuthenticated returns true when user signed in`() = runTest {
        // Given
        repository.signUp("test@example.com", "password123", "Test User")
        
        // Then
        assertTrue(repository.isAuthenticated())
    }
    
    @Test
    fun `isAuthenticated returns false when no user`() = runTest {
        // Then
        assertFalse(repository.isAuthenticated())
    }
}

