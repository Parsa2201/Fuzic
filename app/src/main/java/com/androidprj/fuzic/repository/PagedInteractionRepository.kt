package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.SongItem
import kotlinx.coroutines.flow.Flow

interface PagedInteractionRepository {
    fun observeLikedSongs(userId: String): Flow<PagingData<SongItem>>
    fun observeRecentlyPlayed(userId: String): Flow<PagingData<SongItem>>
}
