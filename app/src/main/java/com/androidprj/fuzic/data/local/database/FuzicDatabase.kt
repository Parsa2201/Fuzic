package com.androidprj.fuzic.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidprj.fuzic.data.local.dao.DownloadDao
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.dao.HomeSongDao
import com.androidprj.fuzic.data.local.dao.UserProfileDao
import com.androidprj.fuzic.data.local.entity.DownloadEntity
import com.androidprj.fuzic.data.local.entity.SearchHistoryEntity
import com.androidprj.fuzic.data.local.entity.HomeSongEntity
import com.androidprj.fuzic.data.local.entity.UserProfileEntity

@Database(
    entities = [
        SearchHistoryEntity::class,
        DownloadEntity::class,
        HomeSongEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FuzicDatabase : RoomDatabase() {
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val downloadDao: DownloadDao
    abstract val homeSongDao: HomeSongDao
    abstract val userProfileDao: UserProfileDao
}
