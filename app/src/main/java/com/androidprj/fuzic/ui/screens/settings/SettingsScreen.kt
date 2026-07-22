package com.androidprj.fuzic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.model.ui.SettingsOverlay
import com.androidprj.fuzic.model.ui.SettingsUiState
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun SettingsRoute(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    onLogoutDismiss: () -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onThemeClick = onThemeClick,
        onLanguageClick = onLanguageClick,
        onLogoutClick = onLogoutClick,
        onLogoutConfirm = onLogoutConfirm,
        onLogoutDismiss = onLogoutDismiss,
        onThemeSelected = onThemeSelected,
        onLanguageSelected = onLanguageSelected,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    onLogoutDismiss: () -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(
            title = stringResource(R.string.settings_title),
            onBackClick = onBackClick,
        )
        when {
            uiState.isLoading -> SettingsLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.settings_error_title),
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
            else -> SettingsContent(
                uiState = uiState,
                onThemeClick = onThemeClick,
                onLanguageClick = onLanguageClick,
                onLogoutClick = onLogoutClick,
            )
        }
    }
    when (uiState.selectedOverlay) {
        SettingsOverlay.Theme -> ThemeSelectionDialog(
            selected = uiState.theme,
            onDismiss = onThemeClick,
            onSelected = onThemeSelected,
        )
        SettingsOverlay.Language -> LanguageSelectionDialog(
            selected = uiState.language,
            onDismiss = onLanguageClick,
            onSelected = onLanguageSelected,
        )
        SettingsOverlay.None -> Unit
    }
    if (uiState.isLogoutConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onLogoutDismiss,
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_logout_title)) },
            text = { Text(stringResource(R.string.settings_logout_message)) },
            confirmButton = {
                TextButton(onClick = onLogoutConfirm) {
                    Text(stringResource(R.string.settings_logout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            SettingsSection(
                title = stringResource(R.string.settings_appearance_section),
                icon = Icons.Default.Settings,
            ) {
                SettingsRow(
                    icon = Icons.Default.LightMode,
                    title = stringResource(R.string.settings_theme_title),
                    value = uiState.theme.label(),
                    onClick = onThemeClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_language_section),
                icon = Icons.Default.Language,
            ) {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    value = uiState.language.label(),
                    onClick = onLanguageClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(R.string.settings_account_section),
            icon = Icons.AutoMirrored.Filled.Logout,
            ) {
                SettingsRow(
            icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.settings_logout),
                    value = null,
                    onClick = onLogoutClick,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            androidx.compose.material3.HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = { Text(title, color = tint) },
        supportingContent = value?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SettingsLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        repeat(3) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.small)
                    .height(SettingsSizes.LoadingRowHeight)
                    .fuzicShimmer(MaterialTheme.shapes.medium),
            )
        }
    }
}

private object SettingsSizes {
    val LoadingRowHeight = 72.dp
}

@Composable
private fun AppThemeOption.label(): String = stringResource(
    when (this) {
        AppThemeOption.System -> R.string.settings_theme_system
        AppThemeOption.Light -> R.string.settings_theme_light
        AppThemeOption.Dark -> R.string.settings_theme_dark
    },
)

@Composable
private fun AppLanguageOption.label(): String = stringResource(
    when (this) {
        AppLanguageOption.System -> R.string.settings_language_system
        AppLanguageOption.English -> R.string.settings_language_english
        AppLanguageOption.Persian -> R.string.settings_language_persian
    },
)

@Composable
private fun ThemeSelectionDialog(
    selected: AppThemeOption,
    onDismiss: () -> Unit,
    onSelected: (AppThemeOption) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column {
                AppThemeOption.entries.forEach { option ->
                    ListItem(
                        headlineContent = { Text(option.label()) },
                        leadingContent = {
                            RadioButton(
                                selected = option == selected,
                                onClick = { onSelected(option) },
                            )
                        },
                        modifier = Modifier.clickable { onSelected(option) },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun LanguageSelectionDialog(
    selected: AppLanguageOption,
    onDismiss: () -> Unit,
    onSelected: (AppLanguageOption) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column {
                AppLanguageOption.entries.forEach { option ->
                    ListItem(
                        headlineContent = { Text(option.label()) },
                        leadingContent = {
                            RadioButton(
                                selected = option == selected,
                                onClick = { onSelected(option) },
                            )
                        },
                        modifier = Modifier.clickable { onSelected(option) },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Preview(name = "Settings - English", showBackground = true)
@Composable
private fun SettingsPreview() {
    FuzicTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                theme = AppThemeOption.Dark,
                language = AppLanguageOption.English,
            ),
            onBackClick = {},
            onThemeClick = {},
            onLanguageClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutDismiss = {},
            onThemeSelected = {},
            onLanguageSelected = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Settings - Persian", locale = "fa", showBackground = true)
@Composable
private fun SettingsPersianPreview() {
    FuzicTheme {
        SettingsScreen(
            uiState = SettingsUiState(),
            onBackClick = {},
            onThemeClick = {},
            onLanguageClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutDismiss = {},
            onThemeSelected = {},
            onLanguageSelected = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Settings logout dialog - Persian", locale = "fa", showBackground = true)
@Composable
private fun SettingsLogoutPreview() {
    FuzicTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLogoutConfirmationVisible = true),
            onBackClick = {},
            onThemeClick = {},
            onLanguageClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutDismiss = {},
            onThemeSelected = {},
            onLanguageSelected = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Settings loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun SettingsLoadingPreview() {
    FuzicTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = true),
            onBackClick = {},
            onThemeClick = {},
            onLanguageClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutDismiss = {},
            onThemeSelected = {},
            onLanguageSelected = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Settings error - Persian", locale = "fa", showBackground = true)
@Composable
private fun SettingsErrorPreview() {
    FuzicTheme {
        SettingsScreen(
            uiState = SettingsUiState(errorMessage = stringResource(R.string.settings_error_message)),
            onBackClick = {},
            onThemeClick = {},
            onLanguageClick = {},
            onLogoutClick = {},
            onLogoutConfirm = {},
            onLogoutDismiss = {},
            onThemeSelected = {},
            onLanguageSelected = {},
            onRetryClick = {},
        )
    }
}
