package com.androidprj.fuzic.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.FeaturedMusicItem
import com.androidprj.fuzic.model.ui.HomeMusicSection
import com.androidprj.fuzic.model.ui.HomeUiState
import com.androidprj.fuzic.model.ui.MusicItemType
import com.androidprj.fuzic.repository.MusicRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            val result = withContext(ioDispatcher) {
                coroutineScope {
                    val daily = async { musicRepository.getDailyPicks() }
                    val popular = async { musicRepository.getMostPopular() }
                    val releases = async { musicRepository.getNewReleases() }
                    val global = async { playlistRepository.getGlobalPlaylists() }
                    val local = async { playlistRepository.getLocalPlaylists() }
                    HomeLoadResult(
                        daily = daily.await(),
                        popular = popular.await(),
                        releases = releases.await(),
                        global = global.await(),
                        local = local.await(),
                    )
                }
            }
            val failure = listOf(result.daily, result.popular, result.releases, result.global, result.local)
                .firstOrNull { it.isFailure }
            if (failure != null) {
                _uiState.value = HomeUiState(
                    errorMessage = failure.exceptionOrNull()?.message
                        ?: stringProvider.get(R.string.home_error_message),
                )
                return@launch
            }
            val daily = result.daily.getOrThrow().map { it.toFeaturedMusicItem() }
            val popular = result.popular.getOrThrow().map { it.toFeaturedMusicItem() }
            val releases = result.releases.getOrThrow().map { it.toFeaturedMusicItem() }
            val global = result.global.getOrThrow().map { it.toFeaturedPlaylistItem() }
            val local = result.local.getOrThrow().map { it.toFeaturedPlaylistItem() }
            _uiState.value = HomeUiState(
                dailyPicks = daily,
                sections = listOf(
                    HomeMusicSection(R.string.home_section_most_popular, popular),
                    HomeMusicSection(R.string.home_section_new_releases, releases),
                    HomeMusicSection(R.string.home_section_global_playlists, global),
                    HomeMusicSection(R.string.home_section_local_playlists, local),
                ),
            )
        }
    }
}

private data class HomeLoadResult(
    val daily: Result<List<com.androidprj.fuzic.model.ui.SongItem>>,
    val popular: Result<List<com.androidprj.fuzic.model.ui.SongItem>>,
    val releases: Result<List<com.androidprj.fuzic.model.ui.SongItem>>,
    val global: Result<List<com.androidprj.fuzic.model.ui.PlaylistItem>>,
    val local: Result<List<com.androidprj.fuzic.model.ui.PlaylistItem>>,
)

private fun com.androidprj.fuzic.model.ui.SongItem.toFeaturedMusicItem() = FeaturedMusicItem(
    id = id,
    title = title,
    subtitle = artist,
    artworkUrl = artworkUrl,
    type = MusicItemType.Song,
)

private fun com.androidprj.fuzic.model.ui.PlaylistItem.toFeaturedPlaylistItem() = FeaturedMusicItem(
    id = id,
    title = title,
    subtitle = subtitle,
    artworkUrl = artworkUrl,
    type = MusicItemType.Playlist,
)
