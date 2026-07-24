package com.androidprj.fuzic.model.ui

data class UpdatePlaylistRequest(
    val title: String,
    val coverImageUrl: String?,
    val category: PlaylistCategory = PlaylistCategory.Local,
    val visibility: PlaylistVisibility = PlaylistVisibility.Public
)
