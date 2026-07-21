package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, name: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun getCurrentUserFlow(): Flow<User?>
    suspend fun getCurrentUserId(): String?
}
