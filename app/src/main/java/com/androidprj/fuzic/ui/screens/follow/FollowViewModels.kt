package com.androidprj.fuzic.ui.screens.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.FollowListType
import com.androidprj.fuzic.model.ui.FollowListUiState
import com.androidprj.fuzic.model.ui.FollowSearchUiState
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.repository.FollowRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface FollowSearchIntent {
    data class QueryChanged(val value: String) : FollowSearchIntent
    data class ToggleFollow(val user: FollowUser) : FollowSearchIntent
    data object Retry : FollowSearchIntent
    data object ClearError : FollowSearchIntent
}

sealed interface FollowListIntent {
    data class Load(val userId: String, val type: FollowListType) : FollowListIntent
    data object Retry : FollowListIntent
    data class ToggleFollow(val user: FollowUser) : FollowListIntent
    data object ClearError : FollowListIntent
}

@HiltViewModel
class FollowSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FollowSearchUiState())
    val uiState: StateFlow<FollowSearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onIntent(intent: FollowSearchIntent) {
        when (intent) {
            is FollowSearchIntent.QueryChanged -> {
                _uiState.value = _uiState.value.copy(query = intent.value, errorMessage = null)
                scheduleSearch()
            }
            is FollowSearchIntent.ToggleFollow -> toggleFollow(intent.user)
            FollowSearchIntent.Retry -> scheduleSearch(immediate = true)
            FollowSearchIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            val query = _uiState.value.query.trim()
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, errorMessage = null)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(ioDispatcher) { userRepository.searchUsers(query) }
            _uiState.value = result.fold(
                onSuccess = { users ->
                    _uiState.value.copy(
                        results = users.map { it.toFollowUser() },
                        isLoading = false,
                    )
                },
                onFailure = {
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: stringProvider.get(R.string.follow_error_title),
                    )
                },
            )
        }
    }

    private fun toggleFollow(user: FollowUser) {
        if (user.isCurrentUser) return
        val previous = _uiState.value.results
        _uiState.value = _uiState.value.copy(
            results = previous.map { if (it.id == user.id) it.copy(isFollowing = !it.isFollowing) else it },
            errorMessage = null,
        )
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (user.isFollowing) followRepository.unfollowUser(user.id) else followRepository.followUser(user.id)
            }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    results = previous,
                    errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.follow_error_title),
                )
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FollowListUiState(type = FollowListType.Followers, isLoading = true))
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()
    private var lastUserId: String? = null
    private var lastType: FollowListType = FollowListType.Followers

    fun onIntent(intent: FollowListIntent) {
        when (intent) {
            is FollowListIntent.Load -> load(intent.userId, intent.type)
            FollowListIntent.Retry -> lastUserId?.let { load(it, lastType) }
            is FollowListIntent.ToggleFollow -> toggleFollow(intent.user)
            FollowListIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    fun load(userId: String, type: FollowListType) {
        lastUserId = userId
        lastType = type
        viewModelScope.launch {
            _uiState.value = FollowListUiState(type = type, isLoading = true)
            val result = withContext(ioDispatcher) {
                if (type == FollowListType.Followers) followRepository.getFollowers(userId)
                else followRepository.getFollowing(userId)
            }
            _uiState.value = result.fold(
                onSuccess = { FollowListUiState(type = type, users = it) },
                onFailure = {
                    FollowListUiState(
                        type = type,
                        errorMessage = it.message ?: stringProvider.get(R.string.follow_error_title),
                    )
                },
            )
        }
    }

    private fun toggleFollow(user: FollowUser) {
        if (user.isCurrentUser) return
        val previous = _uiState.value.users
        _uiState.value = _uiState.value.copy(
            users = previous.map { if (it.id == user.id) it.copy(isFollowing = !it.isFollowing) else it },
            errorMessage = null,
        )
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (user.isFollowing) followRepository.unfollowUser(user.id) else followRepository.followUser(user.id)
            }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    users = previous,
                    errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.follow_error_title),
                )
            }
        }
    }
}

private fun com.androidprj.fuzic.model.ui.ProfileUser.toFollowUser(): FollowUser = FollowUser(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
)
