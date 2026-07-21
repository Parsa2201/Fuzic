package com.androidprj.fuzic.model

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class PremiumUiState(
    val plans: List<PremiumPlan> = emptyList(),
    val selectedPlanId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAlreadyPremium: Boolean = false,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && plans.isEmpty() && !isAlreadyPremium
}

data class PremiumPlan(
    val id: String,
    val title: String,
    val priceLabel: String,
    val billingLabel: String,
    val isRecommended: Boolean = false,
)

enum class PremiumDialogMode {
    Upgrade,
    DownloadBlocked,
}

data class PremiumFeature(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)
