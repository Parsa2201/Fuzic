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
import com.androidprj.fuzic.repository.SettingsRepository
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.repository.SearchRepository
import com.androidprj.fuzic.data.settings.LocalSettingsRepository
import com.androidprj.fuzic.data.settings.LocalPremiumRepository
import com.androidprj.fuzic.data.remote.repository.RemoteSearchRepository
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.data.local.repository.LocalDownloadRepository
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
    abstract fun bindDownloadRepository(
        localDownloadRepository: LocalDownloadRepository
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        remoteChatRepository: RemoteChatRepository
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        localSettingsRepository: LocalSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPremiumRepository(
        localPremiumRepository: LocalPremiumRepository
    ): PremiumRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        remoteSearchRepository: RemoteSearchRepository
    ): SearchRepository
}
