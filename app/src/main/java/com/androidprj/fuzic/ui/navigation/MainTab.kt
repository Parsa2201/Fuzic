package com.androidprj.fuzic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.androidprj.fuzic.R

enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Home(R.string.nav_home, Icons.Default.Home),
    Search(R.string.nav_search, Icons.Default.Search),
    Downloads(R.string.nav_downloads, Icons.Default.Download),
    Playlists(R.string.nav_playlists, Icons.AutoMirrored.Filled.PlaylistPlay),
    Profile(R.string.nav_profile, Icons.Default.Person)
}
