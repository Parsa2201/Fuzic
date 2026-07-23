package com.androidprj.fuzic.di

import android.content.Context
import androidx.room.Room
import com.androidprj.fuzic.data.local.dao.SearchHistoryDao
import com.androidprj.fuzic.data.local.database.FuzicDatabase
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
    fun provideFuzicDatabase(
        @ApplicationContext context: Context
    ): FuzicDatabase {
        return Room.databaseBuilder(
            context,
            FuzicDatabase::class.java,
            "fuzic_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(
        database: FuzicDatabase
    ): SearchHistoryDao {
        return database.searchHistoryDao
    }
}
