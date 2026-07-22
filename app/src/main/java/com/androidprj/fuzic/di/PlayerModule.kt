package com.androidprj.fuzic.di

import com.androidprj.fuzic.data.repository.Media3PlayerRepository
import com.androidprj.fuzic.repository.PlayerRepository
import dagger.Binds
import dagger.Module
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
}
