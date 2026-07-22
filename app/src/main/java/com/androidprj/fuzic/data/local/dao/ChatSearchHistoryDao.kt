package com.androidprj.fuzic.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidprj.fuzic.data.local.entity.ChatSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSearchHistoryDao {
    @Query("SELECT * FROM chat_search_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeHistory(limit: Int = 10): Flow<List<ChatSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatSearchHistoryEntity)

    @Query("DELETE FROM chat_search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM chat_search_history")
    suspend fun clear()
}
