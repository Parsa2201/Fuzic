package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.SongItem

/**
 * Offline-aware interaction surface.
 *
 * ## Offline-first schemas (§17 local data)
 *
 * The `liked_songs` and `recently_played` Room tables that back this
 * interface when offline are Sina-owned per AGENTS.md Local Data §17
 * and preview-defined in
 * [com.androidprj.fuzic.data.local.schema.LikedSongsSchema] /
 * [com.androidprj.fuzic.data.local.schema.RecentlyPlayedSchema]. The
 * preview files are the source-of-truth schemas; Room migrations should
 * be generated from them when Bagher's `backend-integration` branch
 * lands.
 *
 * Methods on this surface that write through Room (`likeSong`,
 * `unlikeSong`, `recordPlay`, `removeRecentlyPlayed`) follow the
 * duplicate-suppression rules documented in the player repository
 * (`Media3PlayerRepository.recordPlayListener`): a single record per
 * playback session, not per playback frame.
 */
interface InteractionRepository {
    suspend fun getRecentlyPlayed(userId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>
    suspend fun getLikedSongs(userId: String, offset: Long = 0, limit: Long = 20): Result<List<SongItem>>

    suspend fun recordPlay(songId: String): Result<Unit>
    suspend fun isSongLiked(songId: String): Result<Boolean>
    suspend fun likeSong(songId: String): Result<Unit>
    suspend fun unlikeSong(songId: String): Result<Unit>
    suspend fun removeRecentlyPlayed(songId: String): Result<Unit>
}
