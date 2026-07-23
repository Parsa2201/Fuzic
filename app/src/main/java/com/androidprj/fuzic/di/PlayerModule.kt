package com.androidprj.fuzic.di

import com.androidprj.fuzic.data.repository.Media3PlayerRepository
import com.androidprj.fuzic.player.local.NoopPlaybackDownloadLookup
import com.androidprj.fuzic.player.local.PlaybackDownloadLookup
import com.androidprj.fuzic.player.queue.PlaybackQueue
import com.androidprj.fuzic.repository.PlayerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Owns Media3-specific bindings for the Playback Track.
 * Repositories outside the Playback Track are intentionally not bound here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        impl: Media3PlayerRepository,
    ): PlayerRepository

    /**
     * Default playback-local lookup until Bagher's `backend-integration`
     * branch lands a `RoomPlaybackDownloadLookup` on master. Swap this
     * binding for the Room-backed impl without touching playback code.
     */
    @Binds
    @Singleton
    abstract fun bindPlaybackDownloadLookup(
        impl: NoopPlaybackDownloadLookup,
    ): PlaybackDownloadLookup
}

/**
 * Provides the initial [PlaybackQueue] for [Media3PlayerRepository].
 *
 * PlaybackQueue is intentionally a pure-Kotlin value (no @Inject, no
 * Hilt annotations) so it remains unit-testable in plain JVM. Hilt's KSP
 * processor does not honour Kotlin default parameter values on
 * `@Inject` constructors, so an explicit binding is required. Returning
 * a fresh instance per injection is fine because Media3PlayerRepository
 * is itself `@Singleton` and consumes the queue only as initial state.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackQueueModule {

    @Provides
    fun providePlaybackQueue(): PlaybackQueue = PlaybackQueue()
}
