package com.androidprj.fuzic.player.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [PlaybackDownloadLookup] used while the Room-backed downloads
 * cache lives on Bagher's `backend-integration` branch and is not yet on
 * master. Always reports "not downloaded" so playback falls back to
 * [com.androidprj.fuzic.model.ui.SongItem.audioUrl].
 *
 * This class is deliberately Room-free — it must compile on `feature/playback`
 * without Room on the classpath. When Room lands on master, replace the
 * Hilt binding in [com.androidprj.fuzic.di.PlayerModule] with a
 * `RoomPlaybackDownloadLookup` that delegates to the downloads DAO; the
 * playback-side resolver signature does not change.
 */
@Singleton
class NoopPlaybackDownloadLookup @Inject constructor() : PlaybackDownloadLookup {
    override suspend fun localUriFor(songId: String): String? = null
}
