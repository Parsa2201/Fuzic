package com.androidprj.fuzic.di

import android.content.Context
import coil.ImageLoader
import com.androidprj.fuzic.data.repository.Media3PlayerRepository
import com.androidprj.fuzic.player.local.PlaybackDownloadLookup
import com.androidprj.fuzic.player.local.RoomPlaybackDownloadLookup
import com.androidprj.fuzic.player.queue.PlaybackQueue
import com.androidprj.fuzic.repository.PlayerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

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
     * Resolves a downloaded song id to its absolute local file path via
     * the downloads DAO. Wires the playback track to Room on
     * `backend-integration`; see [RoomPlaybackDownloadLookup] KDoc for
     * the contract (no filesystem checks, returns null on missing rows).
     */
    @Binds
    @Singleton
    abstract fun bindPlaybackDownloadLookup(
        impl: RoomPlaybackDownloadLookup,
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

/**
 * Provides the process-scoped Coil [ImageLoader] for [Media3PlayerRepository]'s
 * dominant-color extractor. Coil is already on the classpath via the
 * Compose integration; this binding avoids introducing
 * `coil-singleton` as a new dependency.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader = ImageLoader.Builder(context).build()
}

/**
 * Provides the dispatcher used by the crossfade controller's coroutine
 * scope. Injecting via a `@Named` qualifier keeps the production wiring
 * `Dispatchers.Main.immediate` while letting JVM unit tests inject
 * `Dispatchers.Unconfined` (or any other test dispatcher) via the same
 * constructor parameter.
 */
@Module
@InstallIn(SingletonComponent::class)
object CrossfadeDispatcherModule {

    @Provides
    @Named("crossfadeMainDispatcher")
    fun provideCrossfadeMainDispatcher(): CoroutineDispatcher =
        Dispatchers.Main.immediate
}

