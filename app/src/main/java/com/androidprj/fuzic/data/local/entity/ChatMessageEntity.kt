package com.androidprj.fuzic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String?,
    val type: ChatMessageType,
    val sharedSongId: String?,
    val status: ChatMessageStatus,
    val createdAtEpochMillis: Long
)
