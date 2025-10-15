package com.ssbmax.core.data.repository

import com.ssbmax.core.domain.model.User
import com.ssbmax.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of AuthRepository
 * TODO: Replace with Firebase Auth when ready
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    // Mock user storage (in-memory)
    private val users = mutableMapOf<String, Pair<String, User>>() // email -> (password, user)
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val stored = users[email]
            if (stored != null && stored.first == password) {
                _currentUser.value = stored.second
                Result.success(stored.second)
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            if (users.containsKey(email)) {
                return Result.failure(Exception("Email already registered"))
            }
            
            val user = User(
                id = java.util.UUID.randomUUID().toString(),
                email = email,
                displayName = displayName,
                isPremium = false
            )
            
            users[email] = Pair(password, user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return _currentUser.value != null
    }
}

