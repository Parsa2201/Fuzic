package com.androidprj.fuzic.model.ui

data class AppSettings(
    val theme: AppThemeOption = AppThemeOption.System,
    val language: AppLanguageOption = AppLanguageOption.System,
)

data class DownloadRequest(
    val song: SongItem,
    val audioUrl: String,
)

/**
 * App-level playlist creation input. Repository implementations decide how this maps to
 * remote rows, local drafts, uploads, or cached records.
 */
data class CreatePlaylistRequest(
    val title: String,
    val visibility: PlaylistVisibility = PlaylistVisibility.Private,
    val coverImageUrl: String? = null,
)

enum class PlaylistVisibility {
    Public,
    Private,
}

data class TypingStatus(
    val conversationId: String,
    val userId: String,
    val updatedAtEpochMillis: Long,
)

data class ArtistDetails(
    val artist: ArtistItem,
    val popularSongs: List<SongItem> = emptyList(),
)

data class AudioVisualizerFrame(
    val amplitudes: List<Float>,
    val timestampEpochMillis: Long,
)
