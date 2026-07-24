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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import com.androidprj.fuzic.model.remote.PlaylistDto
import com.androidprj.fuzic.model.remote.UserDto
import com.androidprj.fuzic.model.mapper.toSongItem

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

@Singleton class PlaylistDetailsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : PlaylistDetailsRepository { 
    override suspend fun getPlaylistDetails(playlistId: String): Result<PlaylistDetails> {
        return try {
            val playlist = supabaseClient.postgrest["playlists"]
                .select { filter { eq("id", playlistId) } }
                .decodeSingle<PlaylistDto>()
                
            val owner = supabaseClient.postgrest["users"]
                .select { filter { eq("id", playlist.ownerId) } }
                .decodeSingleOrNull<UserDto>()
                
            val songs = supabaseClient.postgrest["playlist_songs"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("song_id, songs(*)")) {
                    filter { eq("playlist_id", playlistId) }
                }
                .decodeList<SongWrapper>()
                .map { it.song.toSongItem() }

            val category = when (playlist.type) { "global" -> com.androidprj.fuzic.model.ui.PlaylistCategory.Global; "local" -> com.androidprj.fuzic.model.ui.PlaylistCategory.Local; else -> com.androidprj.fuzic.model.ui.PlaylistCategory.None }
            val visibility = if (playlist.isPublic) com.androidprj.fuzic.model.ui.PlaylistVisibility.Public else com.androidprj.fuzic.model.ui.PlaylistVisibility.Private

            Result.success(PlaylistDetails(
                id = playlist.id,
                title = playlist.title,
                description = "",
                artworkUrl = playlist.coverImageUrl,
                category = category,
                visibility = visibility,
                ownerId = playlist.ownerId,
                ownerName = owner?.name ?: owner?.username ?: "Unknown",
                songs = songs
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @kotlinx.serialization.Serializable
    private data class SongWrapper(
        @kotlinx.serialization.SerialName("songs") val song: com.androidprj.fuzic.model.remote.SongDto
    )
}

@Singleton class ArtistRepositoryImpl @Inject constructor() : ArtistRepository { override suspend fun getArtist(artistId: String) = unavailableValue<ArtistItem>(); override suspend fun getArtistDetails(artistId: String) = unavailableValue<ArtistDetails>(); override fun observeArtists() = flowOf(PagingData.empty<ArtistCollectionItem>()) }

@Singleton class NotificationRepositoryImpl @Inject constructor() : NotificationRepository { override fun observeNotifications() = flowOf(PagingData.empty<NotificationItem>()); override suspend fun markNotificationAsRead(notificationId: String) = unavailable(); override suspend fun markAllNotificationsAsRead() = unavailable() }
