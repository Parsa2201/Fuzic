package com.androidprj.fuzic.data.remote.repository

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidprj.fuzic.BuildConfig
import com.androidprj.fuzic.data.local.database.AppDatabase
import com.androidprj.fuzic.model.ui.SearchFilter
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteSearchRepositoryIntegrationTest {

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var db: AppDatabase
    private lateinit var searchRepository: RemoteSearchRepository

    @Before
    fun setup() {
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }

        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        searchRepository = RemoteSearchRepository(supabaseClient, db.searchHistoryDao())
    }

    @Test
    fun testSearchHistory() = runBlocking {
        val query1 = "Test Query 1"
        val query2 = "Test Query 2"

        // Save
        assertTrue(searchRepository.saveSearchQuery(query1).isSuccess)
        assertTrue(searchRepository.saveSearchQuery(query2).isSuccess)

        // Observe
        val history = searchRepository.observeSearchHistory().first()
        assertTrue("History should contain saved queries", history.contains(query1))
        assertTrue("History should contain saved queries", history.contains(query2))

        // Delete
        assertTrue(searchRepository.deleteSearchQuery(query1).isSuccess)
        val historyAfterDelete = searchRepository.observeSearchHistory().first()
        assertTrue("History should not contain deleted query", !historyAfterDelete.contains(query1))

        // Clear
        assertTrue(searchRepository.clearSearchHistory().isSuccess)
        val historyAfterClear = searchRepository.observeSearchHistory().first()
        assertTrue("History should be empty", historyAfterClear.isEmpty())
    }

    @Test
    fun testSearch() = runBlocking {
        // We will just verify it doesn't crash when querying the backend
        val filters = listOf(SearchFilter.All, SearchFilter.Songs, SearchFilter.Artists, SearchFilter.Playlists, SearchFilter.Users)

        for (filter in filters) {
            val items = searchRepository.search("test", filter).asSnapshot {
                scrollTo(index = 0)
            }
            assertNotNull("Snapshot should not be null for filter $filter", items)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }
}
