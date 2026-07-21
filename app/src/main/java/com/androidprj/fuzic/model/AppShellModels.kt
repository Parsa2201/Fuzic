package com.androidprj.fuzic.model

data class MiniPlayerUiState(
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false
)
