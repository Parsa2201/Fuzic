package com.androidprj.fuzic.di

import com.androidprj.fuzic.data.remote.repository.RemoteAuthRepository
import com.androidprj.fuzic.data.remote.repository.RemoteChatRepository
import com.androidprj.fuzic.data.remote.repository.RemoteFollowRepository
import com.androidprj.fuzic.data.remote.repository.RemoteInteractionRepository
import com.androidprj.fuzic.data.remote.repository.RemoteMusicRepository
import com.androidprj.fuzic.data.remote.repository.RemotePlaylistRepository
import com.androidprj.fuzic.data.remote.repository.RemoteUserRepository
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.ChatRepository
import com.androidprj.fuzic.repository.FollowRepository
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.repository.MusicRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        remoteAuthRepository: RemoteAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        remoteUserRepository: RemoteUserRepository
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        remoteMusicRepository: RemoteMusicRepository
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        remotePlaylistRepository: RemotePlaylistRepository
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindInteractionRepository(
        remoteInteractionRepository: RemoteInteractionRepository
    ): InteractionRepository

    @Binds
    @Singleton
    abstract fun bindFollowRepository(
        remoteFollowRepository: RemoteFollowRepository
    ): FollowRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        remoteChatRepository: RemoteChatRepository
    ): ChatRepository
}
