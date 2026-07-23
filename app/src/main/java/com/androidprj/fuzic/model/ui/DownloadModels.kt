package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class DownloadsUiState(
    val downloads: List<DownloadedSongItem> = emptyList(),
    val sortOption: DownloadSortOption = DownloadSortOption.DateDownloaded,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isStorageFull: Boolean = false,
    val isPremiumUser: Boolean = false,
    val isPremiumLoading: Boolean = true,
    val isUpgrading: Boolean = false,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && downloads.isEmpty()
}

data class DownloadedSongItem(
    val id: String,
    val title: String,
    val artist: String,
    val fileSizeLabel: String,
    val downloadedAtLabel: String,
    val artworkUrl: String? = null,
    val isDownloadInProgress: Boolean = false,
    val progress: Float = 0f,
    /** Non-null when the file has been fully downloaded to device storage. */
    val localFilePath: String? = null,
)

enum class DownloadSortOption(@StringRes val labelRes: Int) {
    DateDownloaded(R.string.downloads_sort_date),
    Title(R.string.downloads_sort_title),
    Artist(R.string.downloads_sort_artist)
}
