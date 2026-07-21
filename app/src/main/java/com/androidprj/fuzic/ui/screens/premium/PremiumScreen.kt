package com.androidprj.fuzic.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.PremiumDialogMode
import com.androidprj.fuzic.model.ui.PremiumFeature
import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.model.ui.PremiumUiState
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun PremiumRoute(
    uiState: PremiumUiState,
    onPlanSelected: (PremiumPlan) -> Unit,
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        uiState = uiState,
        onPlanSelected = onPlanSelected,
        onUpgradeClick = onUpgradeClick,
        onRestoreClick = onRestoreClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun PremiumScreen(
    uiState: PremiumUiState,
    onPlanSelected: (PremiumPlan) -> Unit,
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> PremiumLoadingContent(modifier)
        uiState.errorMessage != null -> ScreenMessage(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.premium_error_title),
            message = uiState.errorMessage,
            action = {
                Button(onClick = onRetryClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.action_retry))
                }
            },
            modifier = modifier,
        )
        uiState.isAlreadyPremium -> PremiumActiveContent(modifier)
        else -> PremiumContent(
            uiState = uiState,
            onPlanSelected = onPlanSelected,
            onUpgradeClick = onUpgradeClick,
            onRestoreClick = onRestoreClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun PremiumContent(
    uiState: PremiumUiState,
    onPlanSelected: (PremiumPlan) -> Unit,
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            PremiumHero()
        }
        item {
            PremiumFeatures()
        }
        item {
            Text(
                text = stringResource(R.string.premium_choose_plan),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(uiState.plans, key = { it.id }) { plan ->
            PremiumPlanCard(
                plan = plan,
                selected = plan.id == uiState.selectedPlanId,
                onClick = { onPlanSelected(plan) },
            )
        }
        item {
            Button(
                onClick = onUpgradeClick,
                enabled = uiState.selectedPlanId != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.premium_upgrade))
            }
        }
        item {
            TextButton(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.premium_restore_purchase))
            }
        }
    }
}

@Composable
private fun PremiumHero(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.premium_subtitle),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PremiumFeatures(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        premiumFeatures.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(feature.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(feature.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumPlanCard(
    plan: PremiumPlan,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Text(plan.title, style = MaterialTheme.typography.titleMedium)
                    if (plan.isRecommended) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.premium_recommended),
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.spacing.small,
                                    vertical = MaterialTheme.spacing.extraSmall,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Text(
                    text = plan.billingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(plan.priceLabel, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PremiumLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(height = MaterialTheme.spacing.extraLarge, width = MaterialTheme.spacing.extraLarge),
            ) {}
        }
    }
}

@Composable
private fun PremiumActiveContent(modifier: Modifier = Modifier) {
    ScreenMessage(
        icon = Icons.Default.WorkspacePremium,
        title = stringResource(R.string.premium_active_title),
        message = stringResource(R.string.premium_active_message),
        modifier = modifier,
    )
}

@Composable
fun PremiumUpgradeDialog(
    mode: PremiumDialogMode,
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val downloadBlocked = mode == PremiumDialogMode.DownloadBlocked
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (downloadBlocked) Icons.Default.CloudDownload else Icons.Default.WorkspacePremium,
                contentDescription = null,
            )
        },
        title = {
            Text(
                stringResource(
                    if (downloadBlocked) R.string.premium_download_blocked_title
                    else R.string.premium_dialog_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    if (downloadBlocked) R.string.premium_download_blocked_message
                    else R.string.premium_dialog_message,
                ),
            )
        },
        confirmButton = {
            Button(onClick = onUpgradeClick) {
                Text(stringResource(R.string.premium_view_plans))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private val premiumFeatures = listOf(
    PremiumFeature(R.string.premium_feature_downloads, R.string.premium_feature_downloads_description),
    PremiumFeature(R.string.premium_feature_audio, R.string.premium_feature_audio_description),
    PremiumFeature(R.string.premium_feature_badge, R.string.premium_feature_badge_description),
)

@Composable
private fun samplePremiumState() = PremiumUiState(
    plans = listOf(
        PremiumPlan(
            "monthly",
            stringResource(R.string.preview_premium_monthly),
            stringResource(R.string.preview_premium_monthly_price),
            stringResource(R.string.preview_premium_monthly_billing),
        ),
        PremiumPlan(
            "yearly",
            stringResource(R.string.preview_premium_yearly),
            stringResource(R.string.preview_premium_yearly_price),
            stringResource(R.string.preview_premium_yearly_billing),
            isRecommended = true,
        ),
    ),
    selectedPlanId = "yearly",
)

@Preview(name = "Premium - English", showBackground = true)
@Composable
private fun PremiumPreview() {
    FuzicTheme {
        PremiumScreen(
            uiState = samplePremiumState(),
            onPlanSelected = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Premium - Persian", locale = "fa", showBackground = true)
@Composable
private fun PremiumPersianPreview() {
    FuzicTheme {
        PremiumScreen(
            uiState = samplePremiumState(),
            onPlanSelected = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Premium loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun PremiumLoadingPreview() {
    FuzicTheme {
        PremiumScreen(
            uiState = PremiumUiState(isLoading = true),
            onPlanSelected = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Premium error - Persian", locale = "fa", showBackground = true)
@Composable
private fun PremiumErrorPreview() {
    FuzicTheme {
        PremiumScreen(
            uiState = PremiumUiState(
                errorMessage = stringResource(R.string.preview_premium_error_message),
            ),
            onPlanSelected = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Premium active - Persian", locale = "fa", showBackground = true)
@Composable
private fun PremiumActivePreview() {
    FuzicTheme {
        PremiumScreen(
            uiState = PremiumUiState(isAlreadyPremium = true),
            onPlanSelected = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Download blocked dialog - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadBlockedDialogPreview() {
    FuzicTheme {
        PremiumUpgradeDialog(
            mode = PremiumDialogMode.DownloadBlocked,
            onUpgradeClick = {},
            onDismiss = {},
        )
    }
}
