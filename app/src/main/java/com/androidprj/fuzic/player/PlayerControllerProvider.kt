package com.androidprj.fuzic.player

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt accessor for the process-scoped [PlayerController].
 *
 * [PlayerController] already declares `@Singleton` and an `@Inject` constructor,
 * so Hilt can provide it directly. This module exists so downstream consumers
 * have a stable, explicit binding point and so future playback increments can
 * swap in qualifiers, alternate implementations, or test fakes without
 * changing call sites.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerControllerProvider {
    @Provides
    @Singleton
    fun providePlayerController(controller: PlayerController): PlayerController = controller
}