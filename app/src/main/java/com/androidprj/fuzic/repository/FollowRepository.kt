package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.FollowUser
import kotlinx.coroutines.flow.Flow

/** Follow mutations/counts and summaries; long list UI must use [PagedFollowRepository]. */
interface FollowRepository {
    suspend fun followUser(followeeId: String): Result<Unit>
    suspend fun unfollowUser(followeeId: String): Result<Unit>
    
    suspend fun getFollowers(userId: String, offset: Long = 0, limit: Long = 20): Result<List<FollowUser>>
    suspend fun getFollowing(userId: String, offset: Long = 0, limit: Long = 20): Result<List<FollowUser>>
    
    fun observeFollowersCount(userId: String): Flow<Int>
    fun observeFollowingCount(userId: String): Flow<Int>
}
