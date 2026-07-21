package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.User

interface UserRepository {
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun updateProfile(user: User): Result<User>
    suspend fun searchUsers(query: String): Result<List<User>>
}
