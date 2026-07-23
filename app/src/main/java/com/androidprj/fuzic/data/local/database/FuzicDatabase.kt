package com.androidprj.fuzic.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.entity.SearchHistoryEntity

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FuzicDatabase : RoomDatabase() {
    abstract val searchHistoryDao: SearchHistoryDao
}
