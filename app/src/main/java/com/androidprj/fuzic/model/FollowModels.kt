package com.androidprj.fuzic.model

data class FollowUser(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
)

enum class FollowListType {
    Followers,
    Following,
}

data class FollowSearchUiState(
    val query: String = "",
    val results: List<FollowUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && query.isNotBlank() && results.isEmpty()
}

data class FollowListUiState(
    val type: FollowListType,
    val users: List<FollowUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && users.isEmpty()
}
