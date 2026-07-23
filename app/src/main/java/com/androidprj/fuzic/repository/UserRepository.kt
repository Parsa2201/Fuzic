package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.AvatarUploadRequest

interface UserRepository {
    suspend fun getUserProfile(userId: String): Result<ProfileUser>
    suspend fun updateProfile(user: ProfileUser): Result<ProfileUser>
    suspend fun uploadAvatar(
        userId: String,
        request: AvatarUploadRequest,
        onProgress: (Float) -> Unit,
    ): Result<String>
    suspend fun deleteAvatar(userId: String, avatarUrl: String): Result<Unit>
    suspend fun deletePendingAvatar(userId: String, uploadId: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<ProfileUser>>
}
