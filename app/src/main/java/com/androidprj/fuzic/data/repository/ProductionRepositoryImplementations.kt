package com.androidprj.fuzic.data.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.*
import com.androidprj.fuzic.repository.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

private fun unavailable(): Result<Unit> = Result.failure(IllegalStateException("Repository implementation is not configured"))
private fun <T> unavailableValue(): Result<T> = Result.failure(IllegalStateException("Repository implementation is not configured"))

@Singleton class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    override fun observeSettings() = flowOf(AppSettings())
    override suspend fun setTheme(theme: AppThemeOption) = unavailable()
    override suspend fun setLanguage(language: AppLanguageOption) = unavailable()
    override suspend fun clearSettings() = unavailable()
}
@Singleton class PremiumRepositoryImpl @Inject constructor() : PremiumRepository {
    override fun observePremiumStatus() = flowOf(false)
    override suspend fun getPlans() = unavailableValue<List<PremiumPlan>>()
    override suspend fun purchasePlan(planId: String) = unavailable()
    override suspend fun restorePurchase() = unavailable()
}
@Singleton class DownloadRepositoryImpl @Inject constructor() : DownloadRepository {
    override fun observeDownloads(sortOption: DownloadSortOption) = flowOf(emptyList<DownloadedSongItem>())
    override suspend fun enqueueDownload(request: DownloadRequest) = unavailable()
    override suspend fun deleteDownload(downloadId: String) = unavailable()
    override suspend fun restoreDownload(downloadId: String) = unavailable()
    override suspend fun removeDownloadFile(downloadId: String) = unavailable()
}
@Singleton class PlaylistDetailsRepositoryImpl @Inject constructor() : PlaylistDetailsRepository { override suspend fun getPlaylistDetails(playlistId: String) = unavailableValue<PlaylistDetails>() }
@Singleton class ArtistRepositoryImpl @Inject constructor() : ArtistRepository { override suspend fun getArtist(artistId: String) = unavailableValue<ArtistItem>(); override suspend fun getArtistDetails(artistId: String) = unavailableValue<ArtistDetails>(); override fun observeArtists() = flowOf(PagingData.empty<ArtistCollectionItem>()) }
@Singleton class SearchRepositoryImpl @Inject constructor() : SearchRepository { override fun search(query: String, filter: SearchFilter) = flowOf(PagingData.empty<SearchResultItem>()); override fun observeSearchHistory() = flowOf(emptyList<String>()); override suspend fun saveSearchQuery(query: String) = unavailable(); override suspend fun deleteSearchQuery(query: String) = unavailable(); override suspend fun clearSearchHistory() = unavailable() }
@Singleton class NotificationRepositoryImpl @Inject constructor() : NotificationRepository { override fun observeNotifications() = flowOf(PagingData.empty<NotificationItem>()); override suspend fun markNotificationAsRead(notificationId: String) = unavailable(); override suspend fun markAllNotificationsAsRead() = unavailable() }
