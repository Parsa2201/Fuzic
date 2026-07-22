package com.androidprj.fuzic.di

import android.content.Context
import androidx.room.Room
import com.androidprj.fuzic.data.local.dao.ChatDao
import com.androidprj.fuzic.data.local.dao.ChatSearchHistoryDao
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.dao.LocalPlaylistDao
import com.androidprj.fuzic.data.local.dao.MusicCacheDao
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fuzic_database"
        ).build()
    }

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideLocalPlaylistDao(database: AppDatabase): LocalPlaylistDao = database.localPlaylistDao()

    @Provides
    fun provideMusicCacheDao(database: AppDatabase): MusicCacheDao = database.musicCacheDao()

    @Provides
    fun provideChatSearchHistoryDao(database: AppDatabase): ChatSearchHistoryDao = database.chatSearchHistoryDao()
}
