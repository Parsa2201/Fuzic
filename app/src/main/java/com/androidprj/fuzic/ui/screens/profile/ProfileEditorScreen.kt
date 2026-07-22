package com.androidprj.fuzic.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun ProfileEditorScreen(
    uiState: ProfileEditorUiState,
    onBackClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAvatarUrlChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.edit_profile_title), onBackClick = onBackClick)
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(androidx.compose.material3.MaterialTheme.spacing.large))
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.edit_profile_title),
                message = uiState.errorMessage,
                action = { Button(onClick = onRetryClick) { Text(stringResource(R.string.action_retry)) } },
            )
            else -> Column(Modifier.padding(androidx.compose.material3.MaterialTheme.spacing.medium)) {
                OutlinedTextField(uiState.displayName, onDisplayNameChange, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.edit_profile_display_name)) })
                OutlinedTextField(uiState.username, onUsernameChange, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.edit_profile_username)) })
                OutlinedTextField(uiState.avatarUrl, onAvatarUrlChange, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.edit_profile_avatar_url)) })
                Button(onClick = onSaveClick, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isSaving) CircularProgressIndicator() else Text(stringResource(R.string.edit_profile_save))
                }
            }
        }
    }
}

@Preview(name = "Edit profile - English", showBackground = true)
@Composable
private fun ProfileEditorContentPreview() {
    FuzicTheme {
        var state by remember {
            mutableStateOf(
                ProfileEditorUiState(
                    profile = ProfileUser("preview-user", "parsa", "Parsa"),
                    displayName = "Parsa",
                    username = "parsa",
                    isLoading = false,
                ),
            )
        }
        ProfileEditorScreen(
            uiState = state,
            onBackClick = {},
            onDisplayNameChange = { state = state.copy(displayName = it) },
            onUsernameChange = { state = state.copy(username = it) },
            onAvatarUrlChange = { state = state.copy(avatarUrl = it) },
            onSaveClick = { state = state.copy(isSaving = true) },
            onRetryClick = { state = state.copy(errorMessage = null, isLoading = false) },
        )
    }
}

@Preview(name = "Edit profile loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileEditorLoadingPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = ProfileEditorUiState(),
            onBackClick = {}, onDisplayNameChange = {}, onUsernameChange = {}, onAvatarUrlChange = {}, onSaveClick = {}, onRetryClick = {},
        )
    }
}

@Preview(name = "Edit profile error - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileEditorErrorPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = ProfileEditorUiState(isLoading = false, errorMessage = stringResource(R.string.edit_profile_error)),
            onBackClick = {},
            onDisplayNameChange = {},
            onUsernameChange = {},
            onAvatarUrlChange = {},
            onSaveClick = {},
            onRetryClick = {},
        )
    }
}
