package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.mapper.toFollowUser
import com.androidprj.fuzic.model.mapper.toChatMessage
import com.androidprj.fuzic.model.mapper.toChatTimeLabel
import com.androidprj.fuzic.model.mapper.toSongItem
import com.androidprj.fuzic.model.ui.TypingStatus
import com.androidprj.fuzic.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class RemoteChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    override fun observeConversations(): Flow<List<ChatConversation>> = flow {
        emit(loadConversations().getOrThrow())
    }

    override fun observeMessages(conversationId: String): Flow<PagingData<ChatMessage>> {
        val channel = supabaseClient.channel(messageChannel(conversationId))
        return flow<PagingData<ChatMessage>> {
            val messageChanges = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }
            channel.subscribe(blockUntilSubscribed = true)
            emit(PagingData.from(getChatHistory(conversationId).getOrDefault(emptyList())))
            messageChanges.collect { change ->
                val message = change.decodeRecord<MessageDto>()
                if (message.belongsToConversation(conversationId)) {
                    emit(PagingData.from(getChatHistory(conversationId).getOrDefault(emptyList())))
                }
            }
        }.onStart {
            supabaseClient.realtime.connect()
        }.onCompletion {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    override fun observeTypingStatus(conversationId: String): Flow<TypingStatus?> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
        if (currentUserId == null) return flowOf(null)
        val channel = supabaseClient.channel(typingChannel(conversationId))
        return channelFlow {
            // Register the callback before joining the channel. Realtime broadcasts that
            // arrive immediately after the join acknowledgement would otherwise be lost.
            val typingEvents = channel.broadcastFlow<TypingEvent>(TYPING_EVENT)
                .map { event ->
                    if (event.userId != currentUserId && event.isTyping) {
                        TypingStatus(
                            conversationId = conversationId,
                            userId = event.userId,
                            updatedAtEpochMillis = System.currentTimeMillis(),
                        )
                    } else {
                        null
                    }
                }
                .onEach(::send)
                .launchIn(this)
            channel.subscribe(blockUntilSubscribed = true)
            awaitClose { typingEvents.cancel() }
        }.onStart {
            supabaseClient.realtime.connect()
        }.onCompletion {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        receiverId: String,
        text: String
    ): Result<ChatMessage> = sendMessage(
        receiverId = receiverId,
        content = text,
        sharedSongId = null
    )

    override suspend fun sendSongMessage(
        conversationId: String,
        receiverId: String,
        songId: String
    ): Result<ChatMessage> = sendMessage(
        receiverId = receiverId,
        content = null,
        sharedSongId = songId
    )

    override suspend fun markMessagesAsRead(
        conversationId: String,
        messageIds: List<String>
    ): Result<Unit> = runCatching {
        messageIds.forEach { messageId ->
            markMessageAsRead(messageId).getOrThrow()
        }
    }

    override suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("Not logged in")
            val channel = supabaseClient.channel(typingChannel(conversationId))
            // A short-lived typing event must not be put on a websocket queue and then
            // immediately removed. An unsubscribed channel uses Supabase's HTTP broadcast,
            // which delivers the event before this call returns.
            channel.broadcast(
                event = TYPING_EVENT,
                message = TypingEvent(userId = currentUserId, isTyping = isTyping),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshConversation(conversationId: String): Result<Unit> {
        return Result.success(Unit)
    }

    private suspend fun getChatHistory(userId: String, offset: Long = 0, limit: Long = 50): Result<List<ChatMessage>> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
            val messages = supabaseClient.postgrest["messages"]
                .select(columns = MessageWithSongColumns) {
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

    private suspend fun loadConversations(): Result<List<ChatConversation>> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("Not logged in")
            val messages = supabaseClient.postgrest["messages"]
                .select(columns = MessageWithSongColumns) {
                    filter {
                        or {
                            eq("sender_id", currentUserId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<MessageDto>()

            val participants = messages
                .asSequence()
                .map { message ->
                    val participantId = if (message.senderId == currentUserId) {
                        message.receiverId
                    } else {
                        message.senderId
                    }
                    participantId to message
                }
                .distinctBy { it.first }
                .toList()

            val conversations = participants.mapNotNull { (participantId, latestMessage) ->
                    val participant = supabaseClient.postgrest["users"]
                        .select { filter { eq("id", participantId) } }
                        .decodeList<com.androidprj.fuzic.model.remote.UserDto>()
                        .firstOrNull()
                        ?: return@mapNotNull null
                    ChatConversation(
                        id = participantId,
                        participant = participant.toFollowUser(),
                        lastMessagePreview = latestMessage.content ?: latestMessage.sharedSong?.title.orEmpty(),
                        lastMessageTimeLabel = latestMessage.createdAt.toChatTimeLabel(),
                        unreadCount = if (
                            latestMessage.receiverId == currentUserId &&
                            latestMessage.status != "read"
                        ) {
                            1
                        } else {
                            0
                        },
                    )
                }
                .toList()
            Result.success(conversations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendMessage(receiverId: String, content: String?, sharedSongId: String?): Result<ChatMessage> {
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
            val sharedSong = insertedMessage.sharedSongId?.let { songId ->
                getSharedSong(songId)
            }
            Result.success(insertedMessage.toChatMessage(currentUserId, sharedSong))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun markMessageAsRead(messageId: String): Result<Unit> {
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

    private suspend fun getSharedSong(songId: String): SongItem? = runCatching {
        supabaseClient.postgrest["songs"]
            .select { filter { eq("id", songId) } }
            .decodeSingle<SongDto>()
            .toSongItem()
    }.getOrNull()

    private fun observeMessageInserts(userId: String): Flow<ChatMessage> {
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

    private fun MessageDto.belongsToConversation(participantId: String): Boolean {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return false
        return (senderId == currentUserId && receiverId == participantId) ||
            (senderId == participantId && receiverId == currentUserId)
    }

    private fun messageChannel(conversationId: String) = "chat:$conversationId:messages"

    private fun typingChannel(conversationId: String) = "chat:$conversationId:typing"

    @kotlinx.serialization.Serializable
    private data class TypingEvent(
        val userId: String,
        val isTyping: Boolean,
    )

    private companion object {
        const val TYPING_EVENT = "typing"
        val MessageWithSongColumns = Columns.raw("*, shared_song:songs(*)")
    }

    @kotlinx.serialization.Serializable
    private data class InsertMessageDto(
        @kotlinx.serialization.SerialName("sender_id") val senderId: String,
        @kotlinx.serialization.SerialName("receiver_id") val receiverId: String,
        val content: String? = null,
        @kotlinx.serialization.SerialName("shared_song_id") val sharedSongId: String? = null
    )
}
