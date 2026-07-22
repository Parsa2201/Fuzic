package com.androidprj.fuzic.model.mapper

import com.androidprj.fuzic.data.local.entity.ChatMessageEntity
import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType
import kotlinx.datetime.Instant
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun ChatMessageEntity.toChatMessage(currentUserId: String): ChatMessage {
    return ChatMessage(
        id = this.id,
        senderId = this.senderId,
        text = this.text,
        type = this.type,
        song = null, // Requires a separate fetch from MusicRepository, leaving null for now
        status = this.status,
        timeLabel = formatTime(this.createdAtEpochMillis),
        isMine = this.senderId == currentUserId
    )
}

fun MessageDto.toChatMessageEntity(): ChatMessageEntity {
    val status = when {
        this.readAt != null -> ChatMessageStatus.Read
        this.deliveredAt != null -> ChatMessageStatus.Delivered
        else -> ChatMessageStatus.Sent
    }
    
    val type = try {
        ChatMessageType.valueOf(this.messageType ?: "Text")
    } catch (e: Exception) {
        if (this.sharedSongId != null) ChatMessageType.SongShare else ChatMessageType.Text
    }
    
    val epochMillis = try {
        this.createdAt?.let { Instant.parse(it).toEpochMilliseconds() } ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    
    return ChatMessageEntity(
        id = this.id.toString(),
        conversationId = this.conversationId ?: "", // Assuming this is handled or backfilled
        senderId = this.senderId.toString(),
        text = this.content,
        type = type,
        sharedSongId = this.sharedSongId,
        status = status,
        createdAtEpochMillis = epochMillis
    )
}

fun ChatMessage.toChatMessageEntity(conversationId: String, currentUserId: String, sharedSongId: String? = null): ChatMessageEntity {
    return ChatMessageEntity(
        id = this.id,
        conversationId = conversationId,
        senderId = if (this.isMine) currentUserId else this.senderId,
        text = this.text,
        type = this.type,
        sharedSongId = sharedSongId ?: this.song?.id,
        status = this.status,
        createdAtEpochMillis = System.currentTimeMillis()
    )
}
