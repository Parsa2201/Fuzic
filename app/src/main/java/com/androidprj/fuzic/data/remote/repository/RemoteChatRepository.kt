package com.androidprj.fuzic.data.remote.repository

import com.androidprj.fuzic.model.Message
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

    override suspend fun getChatHistory(userId: String, offset: Long, limit: Long): Result<List<Message>> {
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
                .decodeList<Message>()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(receiverId: String, content: String?, sharedSongId: String?): Result<Message> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                sharedSongId = sharedSongId
            )
            val insertedMessage = supabaseClient.postgrest["messages"]
                .insert(message) { select() }
                .decodeSingle<Message>()
            Result.success(insertedMessage)
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

    override fun observeMessages(userId: String): Flow<Message> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw Exception("Not logged in")
            
        val channel = supabaseClient.channel("public:messages")
        return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.map { it.decodeRecord<Message>() }
         .filter { message ->
             (message.senderId == currentUserId && message.receiverId == userId) ||
             (message.senderId == userId && message.receiverId == currentUserId)
         }
    }
}
