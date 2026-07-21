package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.ProfileStats
import com.androidprj.fuzic.model.ui.ProfileUser
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeCurrentProfile(): Flow<ProfileUser?>
    suspend fun getProfile(userId: String): Result<ProfileUser>
    suspend fun updateProfile(profile: ProfileUser): Result<ProfileUser>
    suspend fun getProfileStats(userId: String): Result<ProfileStats>
}
