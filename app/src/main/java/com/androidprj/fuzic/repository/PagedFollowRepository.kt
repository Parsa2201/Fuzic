package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.FollowUser
import kotlinx.coroutines.flow.Flow

interface PagedFollowRepository {
    fun observeFollowers(userId: String): Flow<PagingData<FollowUser>>
    fun observeFollowing(userId: String): Flow<PagingData<FollowUser>>
    fun searchUsers(query: String): Flow<PagingData<FollowUser>>
}
