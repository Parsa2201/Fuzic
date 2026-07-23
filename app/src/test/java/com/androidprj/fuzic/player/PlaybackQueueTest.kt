package com.androidprj.fuzic.player

import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.player.queue.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val songs = listOf(song("a"), song("b"), song("c"))

private fun song(id: String) = SongItem(
    id = id,
    title = "Song $id",
    artist = "Artist $id",
)

class PlaybackQueueTest {

    @Test
    fun `withSongs preserves order and requested playhead`() {
        // Arrange
        val queue = PlaybackQueue()

        // Act
        val updated = queue.withSongs(songs, 0)

        // Assert
        assertEquals(songs, updated.songs)
        assertEquals(0, updated.currentIndex)
    }
    @Test
    fun `withSongs returns an empty queue for empty input`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs)

        // Act
        val updated = queue.withSongs(emptyList())

        // Assert
        assertTrue(updated.songs.isEmpty())
        assertEquals(-1, updated.currentIndex)
    }
    @Test
    fun `withSongs clamps an oversized playhead to the last song`() {
        // Arrange
        val queue = PlaybackQueue()

        // Act
        val updated = queue.withSongs(songs, 5)

        // Assert
        assertEquals(2, updated.currentIndex)
    }
    @Test
    fun `enabling shuffle preserves every song`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, randomSeed = 0L)

        // Act
        val shuffled = queue.withShuffleEnabled(true)

        // Assert
        assertEquals(songs.toSet(), shuffled.songs.toSet())
        assertEquals(songs.size, shuffled.songs.size)
    }
    @Test
    fun `disabling shuffle restores the original order`() {
        // Arrange
        val shuffled = PlaybackQueue(initialSongs = songs, randomSeed = 0L)
            .withShuffleEnabled(true)

        // Act
        val restored = shuffled.withShuffleEnabled(false)

        // Assert
        assertEquals(songs, restored.songs)
    }
    @Test
    fun `withAddedSong appends without moving the playhead`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, initialIndex = 1)
        val newSong = song("d")

        // Act
        val updated = queue.withAddedSong(newSong)

        // Assert
        assertEquals(songs + newSong, updated.songs)
        assertEquals(1, updated.currentIndex)
    }
    @Test
    fun `withRemovedSong removes the current first song and keeps index zero`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, initialIndex = 0)

        // Act
        val updated = queue.withRemovedSong("a")

        // Assert
        assertEquals(listOf(song("b"), song("c")), updated.songs)
        assertEquals(0, updated.currentIndex)
        assertEquals("b", updated.currentSong?.id)
    }
    @Test
    fun `withCleared removes all songs and resets the playhead`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, initialIndex = 1)

        // Act
        val cleared = queue.withCleared()

        // Assert
        assertTrue(cleared.songs.isEmpty())
        assertEquals(-1, cleared.currentIndex)
    }
    @Test
    fun `next advances and stops at the final song when repeat is off`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs)

        // Act
        val advanced = queue.next().next().next()

        // Assert
        assertEquals(2, advanced.currentIndex)
    }
    @Test
    fun `next wraps from last to first when repeating all`() {
        // Arrange
        val queue = PlaybackQueue(songs, 2, initialRepeatMode = RepeatMode.All)

        // Act
        val advanced = queue.next()

        // Assert
        assertEquals(0, advanced.currentIndex)
    }
    @Test
    fun `next keeps the same index when repeating one`() {
        // Arrange
        val queue = PlaybackQueue(songs, 1, initialRepeatMode = RepeatMode.One)

        // Act
        val advanced = queue.next()

        // Assert
        assertEquals(1, advanced.currentIndex)
    }
    @Test
    fun `previous moves backward and stops at the first song when repeat is off`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, initialIndex = 2)

        // Act
        val moved = queue.previous().previous().previous()

        // Assert
        assertEquals(0, moved.currentIndex)
    }
    @Test
    fun `previous wraps from first to last when repeating all`() {
        // Arrange
        val queue = PlaybackQueue(songs, 0, initialRepeatMode = RepeatMode.All)

        // Act
        val moved = queue.previous()

        // Assert
        assertEquals(2, moved.currentIndex)
    }
    @Test
    fun `previous keeps the same index when repeating one`() {
        // Arrange
        val queue = PlaybackQueue(songs, 1, initialRepeatMode = RepeatMode.One)

        // Act
        val moved = queue.previous()

        // Assert
        assertEquals(1, moved.currentIndex)
    }
    @Test
    fun `withRepeatMode changes only the repeat mode`() {
        // Arrange
        val queue = PlaybackQueue(initialSongs = songs, initialIndex = 1)

        // Act
        val updated = queue.withRepeatMode(RepeatMode.All)

        // Assert
        assertEquals(queue.copy(repeatMode = RepeatMode.All), updated)
        assertEquals(RepeatMode.Off, queue.repeatMode)
    }
}
