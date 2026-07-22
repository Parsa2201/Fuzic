package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.FollowUser
import kotlinx.coroutines.flow.Flow

import androidx.paging.PagingData

/** Follow mutations/counts and summaries; long list UI must use [PagedFollowRepository]. */
interface FollowRepository {
    suspend fun followUser(followeeId: String): Result<Unit>
    suspend fun unfollowUser(followeeId: String): Result<Unit>
    
    fun observeFollowers(userId: String): Flow<PagingData<FollowUser>>
    fun observeFollowing(userId: String): Flow<PagingData<FollowUser>>
    
    fun observeFollowersCount(userId: String): Flow<Int>
    fun observeFollowingCount(userId: String): Flow<Int>
}
