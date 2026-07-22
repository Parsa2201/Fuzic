package com.androidprj.fuzic.data.local.converter

import androidx.room.TypeConverter
import com.androidprj.fuzic.model.ui.ChatMessageStatus
import com.androidprj.fuzic.model.ui.ChatMessageType

class Converters {
    @TypeConverter
    fun fromChatMessageStatus(status: ChatMessageStatus): String {
        return status.name
    }

    @TypeConverter
    fun toChatMessageStatus(status: String): ChatMessageStatus {
        return try {
            ChatMessageStatus.valueOf(status)
        } catch (e: Exception) {
            ChatMessageStatus.Sent
        }
    }

    @TypeConverter
    fun fromChatMessageType(type: ChatMessageType): String {
        return type.name
    }

    @TypeConverter
    fun toChatMessageType(type: String): ChatMessageType {
        return try {
            ChatMessageType.valueOf(type)
        } catch (e: Exception) {
            ChatMessageType.Text
        }
    }
}
