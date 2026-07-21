package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.ProfileUser

interface UserRepository {
    suspend fun getUserProfile(userId: String): Result<ProfileUser>
    suspend fun updateProfile(user: ProfileUser): Result<ProfileUser>
    suspend fun searchUsers(query: String): Result<List<ProfileUser>>
}
