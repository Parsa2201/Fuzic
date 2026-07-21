package com.androidprj.fuzic.data.remote.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.ArtistCollectionItem
import com.androidprj.fuzic.model.ui.ArtistDetails
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.repository.ArtistRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryArtistRepository @Inject constructor() : ArtistRepository {
    override suspend fun getArtist(artistId: String): Result<ArtistItem> {
        return Result.failure(IllegalStateException("Artists are not implemented yet"))
    }

    override suspend fun getArtistDetails(artistId: String): Result<ArtistDetails> {
        return Result.failure(IllegalStateException("Artist details are not implemented yet"))
    }

    override fun observeArtists(): Flow<PagingData<ArtistCollectionItem>> = flowOf(PagingData.empty())
}
