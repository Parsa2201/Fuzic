package com.androidprj.fuzic.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.*
import com.androidprj.fuzic.repository.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private fun unavailable(): Result<Unit> = Result.failure(IllegalStateException("Repository implementation is not configured"))
private fun <T> unavailableValue(): Result<T> = Result.failure(IllegalStateException("Repository implementation is not configured"))

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

@Singleton class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override fun observeSettings() = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            theme = preferences.readEnum(SettingsKeys.Theme, AppThemeOption.System),
            language = preferences.readEnum(SettingsKeys.Language, AppLanguageOption.System),
            fontScale = preferences.readEnum(SettingsKeys.FontScale, AppFontScale.Default),
        )
    }

    override suspend fun setTheme(theme: AppThemeOption) = update(SettingsKeys.Theme, theme.name)
    override suspend fun setLanguage(language: AppLanguageOption) = update(SettingsKeys.Language, language.name)
    override suspend fun setFontScale(fontScale: AppFontScale) = update(SettingsKeys.FontScale, fontScale.name)
    override suspend fun clearSettings(): Result<Unit> = runCatching {
        context.settingsDataStore.edit { it.clear() }
        Unit
    }

    private suspend fun update(key: Preferences.Key<String>, value: String): Result<Unit> = runCatching {
        context.settingsDataStore.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> Preferences.readEnum(key: Preferences.Key<String>, default: T): T =
        this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private object SettingsKeys {
        val Theme = stringPreferencesKey("theme")
        val Language = stringPreferencesKey("language")
        val FontScale = stringPreferencesKey("font_scale")
    }
}

@Singleton class PlaylistDetailsRepositoryImpl @Inject constructor() : PlaylistDetailsRepository { override suspend fun getPlaylistDetails(playlistId: String) = unavailableValue<PlaylistDetails>() }
@Singleton class ArtistRepositoryImpl @Inject constructor() : ArtistRepository { override suspend fun getArtist(artistId: String) = unavailableValue<ArtistItem>(); override suspend fun getArtistDetails(artistId: String) = unavailableValue<ArtistDetails>(); override fun observeArtists() = flowOf(PagingData.empty<ArtistCollectionItem>()) }

@Singleton class NotificationRepositoryImpl @Inject constructor() : NotificationRepository { override fun observeNotifications() = flowOf(PagingData.empty<NotificationItem>()); override suspend fun markNotificationAsRead(notificationId: String) = unavailable(); override suspend fun markAllNotificationsAsRead() = unavailable() }
