package com.androidprj.fuzic.model.ui

enum class RepeatMode {
    Off,
    All,
    One,
}

enum class PlayerOverlay {
    None,
    Queue,
    SleepTimer,
    PlaybackSpeed,
    Share,
    AddToPlaylist,
}

data class PlayerUiState(
    val currentSong: SongItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLiked: Boolean = false,
    val progress: Float = 0f,
    val elapsedLabel: String = "0:00",
    val durationLabel: String = "0:00",
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val playbackSpeed: Float = 1f,
    val selectedOverlay: PlayerOverlay = PlayerOverlay.None,
    val queue: List<SongItem> = emptyList(),
    val sleepTimerMinutes: Int? = null,
    val errorMessage: String? = null,
    val visualizerAmplitudes: List<Float> = emptyList(),
    val isPremiumUser: Boolean = false,
    val actionErrorMessage: String? = null,
    /**
     * Dominant color extracted from the current song's cover artwork, in
     * ARGB form. `null` while the artwork is still loading or when no
     * cover URL is present. UI Track consumes this for the full-player
     * background gradient (§130 PDF Source Addendum). Updated by
     * `Media3PlayerRepository` whenever [onMediaItemTransition] fires.
     */
    val dominantColor: Int? = null,
)
