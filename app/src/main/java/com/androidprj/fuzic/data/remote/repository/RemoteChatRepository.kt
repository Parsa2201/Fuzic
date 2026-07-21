package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.mapper.toChatMessage
import com.androidprj.fuzic.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.util.UUID

class RemoteChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    override suspend fun getChatHistory(userId: String, offset: Long, limit: Long): Result<List<ChatMessage>> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val messages = supabaseClient.postgrest["messages"]
                .select {
                    filter {
                        or {
                            and {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", userId)
                            }
                            and {
                                eq("sender_id", userId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    order("created_at", order = Order.ASCENDING)
                    range(offset, offset + limit - 1)
                }
                .decodeList<MessageDto>()
                .map { it.toChatMessage(currentUserId) }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(receiverId: String, content: String?, sharedSongId: String?): Result<ChatMessage> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val message = InsertMessageDto(
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                sharedSongId = sharedSongId
            )
            val insertedMessage = supabaseClient.postgrest["messages"]
                .insert(message) { select() }
                .decodeSingle<MessageDto>()
            Result.success(insertedMessage.toChatMessage(currentUserId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["messages"].update(
                { "status" to "read" }
            ) {
                filter { eq("id", messageId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeMessages(userId: String): Flow<ChatMessage> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw Exception("Not logged in")
            
        val channel = supabaseClient.channel("public:messages")
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.map { it.decodeRecord<MessageDto>() }
         .filter { message ->
             (message.senderId == currentUserId && message.receiverId == userId) ||
             (message.senderId == userId && message.receiverId == currentUserId)
         }
         .map { it.toChatMessage(currentUserId) }
    }

    @kotlinx.serialization.Serializable
    private data class InsertMessageDto(
        @kotlinx.serialization.SerialName("sender_id") val senderId: String,
        @kotlinx.serialization.SerialName("receiver_id") val receiverId: String,
        val content: String? = null,
        @kotlinx.serialization.SerialName("shared_song_id") val sharedSongId: String? = null
    )
}
