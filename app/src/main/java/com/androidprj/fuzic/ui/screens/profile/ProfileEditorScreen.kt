package com.androidprj.fuzic.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.AvatarEditorState
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.ui.components.CircularMusicArtwork
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import java.io.File
import java.util.UUID

@Composable
fun ProfileEditorScreen(
    uiState: ProfileEditorUiState,
    onBackClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onDeleteAvatarClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDiscardChangesClick: () -> Unit,
    onDismissDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onAvatarSelected(it.toString()) }
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.let { onAvatarSelected(it.toString()) }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraUri?.let(cameraCapture::launch)
    }
    val launchCamera: () -> Unit = {
        val file = File(context.cacheDir, "avatar-${UUID.randomUUID()}.jpg")
        cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraUri?.let(cameraCapture::launch)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
        Unit
    }

    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.edit_profile_title), onBackClick = onBackClick)
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(androidx.compose.material3.MaterialTheme.spacing.large))
            uiState.errorMessage != null && uiState.profile == null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.edit_profile_title),
                message = uiState.errorMessage,
                action = { Button(onClick = onRetryClick) { Text(stringResource(R.string.action_retry)) } },
            )
            else -> ProfileEditorContent(
                uiState = uiState,
                onDisplayNameChange = onDisplayNameChange,
                onUsernameChange = onUsernameChange,
                onGalleryClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onCameraClick = launchCamera,
                onDeleteAvatarClick = onDeleteAvatarClick,
                onSaveClick = onSaveClick,
            )
        }
    }
    if (uiState.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDiscardClick,
            title = { Text(stringResource(R.string.edit_profile_discard_title)) },
            text = { Text(stringResource(R.string.edit_profile_discard_message)) },
            confirmButton = { Button(onClick = onDiscardChangesClick) { Text(stringResource(R.string.edit_profile_discard_confirm)) } },
            dismissButton = { OutlinedButton(onClick = onDismissDiscardClick) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ProfileEditorContent(
    uiState: ProfileEditorUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDeleteAvatarClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(androidx.compose.material3.MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.MaterialTheme.spacing.medium),
    ) {
        AvatarEditor(
            avatar = uiState.avatar,
            avatarUrl = uiState.displayedAvatarUrl,
            onGalleryClick = onGalleryClick,
            onCameraClick = onCameraClick,
            onDeleteClick = onDeleteAvatarClick,
        )
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_profile_display_name)) },
        )
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_profile_username)) },
        )
        uiState.errorMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        Button(onClick = onSaveClick, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) CircularProgressIndicator() else Text(stringResource(R.string.edit_profile_save))
        }
    }
}

@Composable
private fun AvatarEditor(
    avatar: AvatarEditorState,
    avatarUrl: String?,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val uploadProgress by animateFloatAsState(
        targetValue = (avatar as? AvatarEditorState.Uploading)?.progress ?: 0f,
        label = "avatar_upload_progress",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularMusicArtwork(
                artworkUrl = avatarUrl,
                fallbackIcon = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            if (avatar is AvatarEditorState.Uploading) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier.size(124.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.MaterialTheme.spacing.small)) {
            Text(
                text = if (avatar is AvatarEditorState.Uploading) stringResource(R.string.edit_profile_avatar_uploading) else stringResource(R.string.edit_profile_avatar_change),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.MaterialTheme.spacing.small)) {
                OutlinedButton(onClick = onGalleryClick, enabled = avatar !is AvatarEditorState.Uploading) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(androidx.compose.material3.MaterialTheme.spacing.small))
                    Text(stringResource(R.string.edit_profile_avatar_gallery))
                }
                IconButton(onClick = onCameraClick, enabled = avatar !is AvatarEditorState.Uploading) {
                    Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.edit_profile_avatar_camera))
                }
                if (avatar !is AvatarEditorState.None) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.edit_profile_avatar_delete))
                    }
                }
            }
        }
    }
}

@Preview(name = "Edit profile - existing avatar", showBackground = true)
@Composable
private fun ProfileEditorExistingPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = previewEditorState(AvatarEditorState.Existing("https://example.test/avatar.jpg")),
            onBackClick = {}, onDisplayNameChange = {}, onUsernameChange = {}, onAvatarSelected = {}, onDeleteAvatarClick = {}, onSaveClick = {}, onRetryClick = {}, onDiscardChangesClick = {}, onDismissDiscardClick = {},
        )
    }
}

@Preview(name = "Edit profile - upload progress - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileEditorUploadingPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = previewEditorState(AvatarEditorState.Uploading("https://example.test/local.jpg", "preview", .62f)),
            onBackClick = {}, onDisplayNameChange = {}, onUsernameChange = {}, onAvatarSelected = {}, onDeleteAvatarClick = {}, onSaveClick = {}, onRetryClick = {}, onDiscardChangesClick = {}, onDismissDiscardClick = {},
        )
    }
}

@Preview(name = "Edit profile - upload error", showBackground = true)
@Composable
private fun ProfileEditorUploadErrorPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = previewEditorState(AvatarEditorState.None).copy(
                errorMessage = stringResource(R.string.edit_profile_error),
            ),
            onBackClick = {}, onDisplayNameChange = {}, onUsernameChange = {}, onAvatarSelected = {}, onDeleteAvatarClick = {}, onSaveClick = {}, onRetryClick = {}, onDiscardChangesClick = {}, onDismissDiscardClick = {},
        )
    }
}

@Preview(name = "Edit profile - staged removal - Persian", locale = "fa", showBackground = true)
@Composable
private fun ProfileEditorRemovalPreview() {
    FuzicTheme {
        ProfileEditorScreen(
            uiState = previewEditorState(AvatarEditorState.Removing("https://example.test/avatar.jpg")),
            onBackClick = {}, onDisplayNameChange = {}, onUsernameChange = {}, onAvatarSelected = {}, onDeleteAvatarClick = {}, onSaveClick = {}, onRetryClick = {}, onDiscardChangesClick = {}, onDismissDiscardClick = {},
        )
    }
}

private fun previewEditorState(avatar: AvatarEditorState) = ProfileEditorUiState(
    profile = ProfileUser("preview-user", "parsa", "Parsa", avatarUrl = (avatar as? AvatarEditorState.Existing)?.url),
    displayName = "Parsa",
    username = "parsa",
    avatar = avatar,
    isLoading = false,
)
