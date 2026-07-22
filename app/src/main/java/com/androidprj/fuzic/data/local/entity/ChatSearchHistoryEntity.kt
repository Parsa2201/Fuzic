package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_search_history")
data class ChatSearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)
