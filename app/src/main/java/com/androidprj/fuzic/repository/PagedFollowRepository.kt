package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.FollowUser
import kotlinx.coroutines.flow.Flow

/** Paging contract for Followers and Following; summary reads remain on [FollowRepository]. */
interface PagedFollowRepository {
    fun observeFollowers(userId: String): Flow<PagingData<FollowUser>>
    fun observeFollowing(userId: String): Flow<PagingData<FollowUser>>
}
