package com.androidprj.fuzic

import com.androidprj.fuzic.model.mapper.toChatMessage
import com.androidprj.fuzic.model.remote.MessageDto
import com.androidprj.fuzic.model.remote.SongDto
import com.androidprj.fuzic.model.ui.ChatMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MessageMapperTest {

    @Test
    fun sharedSongMessageMapsTheEmbeddedSongIntoTheChatCard() {
        val message = MessageDto(
            id = "message-1",
            senderId = "sender-1",
            receiverId = "receiver-1",
            sharedSongId = "song-1",
            sharedSong = SongDto(
                id = "song-1",
                title = "Midnight Drive",
                artistName = "Luna Ray",
            ),
        )

        val mapped = message.toChatMessage(currentUserId = "sender-1")

        assertEquals(ChatMessageType.SongShare, mapped.type)
        assertNotNull(mapped.song)
        assertEquals("song-1", mapped.song?.id)
        assertEquals("Midnight Drive", mapped.song?.title)
    }
}
