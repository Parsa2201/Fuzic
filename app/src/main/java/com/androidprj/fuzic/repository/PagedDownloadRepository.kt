package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import kotlinx.coroutines.flow.Flow

/** Paging contract for the Downloads screen; implementations own Room/worker updates. */
interface PagedDownloadRepository {
    fun observePagedDownloads(sortOption: DownloadSortOption): Flow<PagingData<DownloadedSongItem>>
}
