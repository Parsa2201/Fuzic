package com.androidprj.fuzic.player

import com.androidprj.fuzic.player.local.LocalPlaybackFileResolver
import com.androidprj.fuzic.player.local.PlaybackDownloadLookup
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LocalPlaybackFileResolverTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `resolve returns the absolute path for an existing download`() = runTest {
        // Arrange
        val downloadedFile = tempDir.newFile("song-1.mp3")
        val resolver = resolverFor(downloadedFile.absolutePath)

        // Act
        val resolvedPath = resolver.resolve("song-1")

        // Assert
        assertEquals(downloadedFile.absolutePath, resolvedPath)
    }

    @Test
    fun `resolve returns null when the lookup has no download`() = runTest {
        // Arrange
        val resolver = resolverFor(null)

        // Act
        val resolvedPath = resolver.resolve("song-1")

        // Assert
        assertNull(resolvedPath)
    }

    @Test
    fun `resolve returns null when the downloaded file no longer exists`() = runTest {
        // Arrange
        val missingFile = File(tempDir.root, "missing-song.mp3")
        val resolver = resolverFor(missingFile.absolutePath)

        // Act
        val resolvedPath = resolver.resolve("song-1")

        // Assert
        assertNull(resolvedPath)
    }

    private fun kotlinx.coroutines.test.TestScope.resolverFor(path: String?): LocalPlaybackFileResolver {
        val lookup = object : PlaybackDownloadLookup {
            override suspend fun localUriFor(songId: String): String? = path
        }
        return LocalPlaybackFileResolver(
            lookup = lookup,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
    }
}
