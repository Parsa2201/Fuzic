package com.androidprj.fuzic.model.ui

data class AppSettings(
    val theme: AppThemeOption = AppThemeOption.System,
    val language: AppLanguageOption = AppLanguageOption.System,
)

data class DownloadRequest(
    val song: SongItem,
    val audioUrl: String,
)

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
