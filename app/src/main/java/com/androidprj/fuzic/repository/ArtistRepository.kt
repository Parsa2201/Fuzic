package com.androidprj.fuzic.repository

import androidx.paging.PagingData
import com.androidprj.fuzic.model.ui.ArtistDetails
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.model.ui.ArtistCollectionItem
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    suspend fun getArtist(artistId: String): Result<ArtistItem>
    suspend fun getArtistDetails(artistId: String): Result<ArtistDetails>
    fun observeArtists(): Flow<PagingData<ArtistCollectionItem>>
}
