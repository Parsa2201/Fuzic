package com.androidprj.fuzic.player

import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.player.queue.QueueShuffler
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueShufflerTest {

    @Test
    fun `shuffled returns an empty list for empty input`() {
        // Arrange
        val original = emptyList<SongItem>()

        // Act
        val shuffled = QueueShuffler.shuffled(original, Random(0L))

        // Assert
        assertTrue(shuffled.isEmpty())
    }

    @Test
    fun `shuffled preserves size and elements`() {
        // Arrange
        val original = queueSongs()

        // Act
        val shuffled = QueueShuffler.shuffled(original, Random(42L))

        // Assert
        assertEquals(original.size, shuffled.size)
        assertEquals(original.toSet(), shuffled.toSet())
    }

    @Test
    fun `shuffled is deterministic for the same seed`() {
        // Arrange
        val original = queueSongs()

        // Act
        val first = QueueShuffler.shuffled(original, Random(42L))
        val second = QueueShuffler.shuffled(original, Random(42L))

        // Assert
        assertEquals(first, second)
    }

    @Test
    fun `shuffled can produce different permutations for different seeds`() {
        // Arrange
        val original = queueSongs()

        // Act
        val first = QueueShuffler.shuffled(original, Random(0L))
        val second = QueueShuffler.shuffled(original, Random(1L))

        // Assert
        assertNotEquals(first, second)
    }

    @Test
    fun `isShuffledDifferent returns true when sizes differ`() {
        // Arrange
        val original = queueSongs()
        val shorter = original.dropLast(1)

        // Act
        val isDifferent = QueueShuffler.isShuffledDifferent(original, shorter)

        // Assert
        assertTrue(isDifferent)
    }

    @Test
    fun `isShuffledDifferent returns true when any position differs`() {
        // Arrange
        val original = queueSongs()
        val reordered = listOf(original[1], original[0], original[2])

        // Act
        val isDifferent = QueueShuffler.isShuffledDifferent(original, reordered)

        // Assert
        assertTrue(isDifferent)
    }

    @Test
    fun `isShuffledDifferent returns false for identical lists`() {
        // Arrange
        val original = queueSongs()

        // Act
        val isDifferent = QueueShuffler.isShuffledDifferent(original, original.toList())

        // Assert
        assertFalse(isDifferent)
    }

    private fun queueSongs(): List<SongItem> = listOf(
        queueSong("a"),
        queueSong("b"),
        queueSong("c"),
    )

    private fun queueSong(id: String) = SongItem(
        id = id,
        title = "Song $id",
        artist = "Artist $id",
    )
}
