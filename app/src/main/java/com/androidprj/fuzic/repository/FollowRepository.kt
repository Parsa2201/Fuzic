package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.User
import kotlinx.coroutines.flow.Flow

interface FollowRepository {
    suspend fun followUser(followeeId: String): Result<Unit>
    suspend fun unfollowUser(followeeId: String): Result<Unit>
    
    suspend fun getFollowers(userId: String, offset: Long = 0, limit: Long = 20): Result<List<User>>
    suspend fun getFollowing(userId: String, offset: Long = 0, limit: Long = 20): Result<List<User>>
    
    fun observeFollowersCount(userId: String): Flow<Int>
    fun observeFollowingCount(userId: String): Flow<Int>
}
