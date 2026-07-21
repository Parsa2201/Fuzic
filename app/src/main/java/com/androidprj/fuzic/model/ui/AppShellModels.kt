package com.androidprj.fuzic.model.ui

data class MiniPlayerUiState(
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false
)
