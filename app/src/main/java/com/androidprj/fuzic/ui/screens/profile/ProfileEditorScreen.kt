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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.androidprj.fuzic.R
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
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
