package com.androidprj.fuzic.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.model.ui.DownloadsUiState
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DownloadsIntent {
    data object Retry : DownloadsIntent
    data class SortSelected(val option: DownloadSortOption) : DownloadsIntent
    data class Delete(val item: DownloadedSongItem) : DownloadsIntent
    data object UndoDelete : DownloadsIntent
    data class RemoveFile(val item: DownloadedSongItem) : DownloadsIntent
    data object FreeUpSpace : DownloadsIntent
    data object UpgradeToPremium : DownloadsIntent
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val premiumRepository: PremiumRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadsUiState(isLoading = true))
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var lastDeletedId: String? = null

    init {
        observeDownloads(DownloadSortOption.DateDownloaded)
        observePremiumStatus()
    }

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            DownloadsIntent.Retry -> observeDownloads(_uiState.value.sortOption)
            is DownloadsIntent.SortSelected -> observeDownloads(intent.option)
            is DownloadsIntent.Delete -> deleteDownload(intent.item)
            DownloadsIntent.UndoDelete -> undoDelete()
            is DownloadsIntent.RemoveFile -> removeFile(intent.item)
            DownloadsIntent.FreeUpSpace -> _uiState.update { it.copy(isStorageFull = false, errorMessage = null) }
            DownloadsIntent.UpgradeToPremium -> upgradeToPremium()
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumRepository.observePremiumStatus().collect { isPremium ->
                _uiState.update {
                    it.copy(isPremiumUser = isPremium, isPremiumLoading = false, isUpgrading = false)
                }
            }
        }
    }

    private fun upgradeToPremium() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpgrading = true, errorMessage = null) }
            val result = withContext(ioDispatcher) {
                premiumRepository.purchasePlan(DEMO_PURCHASE_PLAN_ID)
            }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isUpgrading = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: stringProvider.get(R.string.premium_error_title),
                    )
                }
            }
            // On success the premium status flow flips isPremiumUser to true automatically.
        }
    }

    private fun observeDownloads(sortOption: DownloadSortOption) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(sortOption = sortOption, isLoading = true, errorMessage = null) }
            downloadRepository.observeDownloads(sortOption)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: stringProvider.get(R.string.downloads_error_message),
                        )
                    }
                }
                .collect { downloads ->
                    _uiState.update {
                        it.copy(downloads = downloads, sortOption = sortOption, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    private fun deleteDownload(item: DownloadedSongItem) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { downloadRepository.deleteDownload(item.id) }
            if (result.isSuccess) {
                lastDeletedId = item.id
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.downloads_error_message))
                }
            }
        }
    }

    private fun undoDelete() {
        val deletedId = lastDeletedId ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { downloadRepository.restoreDownload(deletedId) }
            _uiState.update {
                if (result.isSuccess) {
                    lastDeletedId = null
                    it.copy(errorMessage = null)
                } else {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.downloads_error_message))
                }
            }
        }
    }

    private fun removeFile(item: DownloadedSongItem) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { downloadRepository.removeDownloadFile(item.id) }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isStorageFull = true,
                        errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.downloads_storage_full_message),
                    )
                }
            }
        }
    }

    private companion object {
        // Demo entitlement only: no real purchase flow. Matches RemotePremiumRepository's plan ids.
        const val DEMO_PURCHASE_PLAN_ID = "yearly"
    }
}
