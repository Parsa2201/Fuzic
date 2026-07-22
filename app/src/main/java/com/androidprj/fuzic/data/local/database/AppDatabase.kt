package com.androidprj.fuzic.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.androidprj.fuzic.data.local.converter.Converters
import com.androidprj.fuzic.data.local.dao.ChatDao
import com.androidprj.fuzic.data.local.dao.ChatSearchHistoryDao
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.dao.LocalPlaylistDao
import com.androidprj.fuzic.data.local.dao.MusicCacheDao
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.entity.ChatMessageEntity
import com.androidprj.fuzic.data.local.entity.ChatSearchHistoryEntity
import com.androidprj.fuzic.data.local.entity.DownloadedSongEntity
import com.androidprj.fuzic.data.local.entity.LikedSongEntity
import com.androidprj.fuzic.data.local.entity.LocalPlaylistEntity
import com.androidprj.fuzic.data.local.entity.LocalPlaylistSongEntity
import com.androidprj.fuzic.data.local.entity.RecentlyPlayedEntity
import com.androidprj.fuzic.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        SearchHistoryEntity::class,
        DownloadedSongEntity::class,
        ChatMessageEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistSongEntity::class,
        LikedSongEntity::class,
        RecentlyPlayedEntity::class,
        ChatSearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chatDao(): ChatDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun musicCacheDao(): MusicCacheDao
    abstract fun chatSearchHistoryDao(): ChatSearchHistoryDao
}
