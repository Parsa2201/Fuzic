package com.androidprj.fuzic.ui.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.model.ui.PremiumUiState
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.androidprj.fuzic.util.toUserFriendlyMessage

sealed interface PremiumIntent {
    data object Retry : PremiumIntent
    data class SelectPlan(val plan: PremiumPlan) : PremiumIntent
    data object Upgrade : PremiumIntent
    data object RestorePurchase : PremiumIntent
    data object ClearError : PremiumIntent
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PremiumUiState(isLoading = true))
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: PremiumIntent) {
        when (intent) {
            PremiumIntent.Retry -> load()
            is PremiumIntent.SelectPlan -> _uiState.value = _uiState.value.copy(
                selectedPlanId = intent.plan.id,
                errorMessage = null,
            )
            PremiumIntent.Upgrade -> purchaseSelectedPlan()
            PremiumIntent.RestorePurchase -> restorePurchase()
            PremiumIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(ioDispatcher) {
                coroutineScope {
                    val status = async { premiumRepository.observePremiumStatus() }
                    val plans = async { premiumRepository.getPlans() }
                    status.await() to plans.await()
                }
            }
            val plans = result.second
            if (plans.isFailure) {
                _uiState.value = PremiumUiState(
                    errorMessage = plans.exceptionOrNull()?.message
                        ?: stringProvider.get(R.string.premium_error_title),
                )
                return@launch
            }
            premiumRepository.observePremiumStatus().collect { isPremium ->
                _uiState.value = _uiState.value.copy(
                    plans = plans.getOrThrow(),
                    selectedPlanId = _uiState.value.selectedPlanId ?: plans.getOrThrow().firstOrNull { it.isRecommended }?.id,
                    isAlreadyPremium = isPremium,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    private fun purchaseSelectedPlan() {
        val selectedPlanId = _uiState.value.selectedPlanId
        if (selectedPlanId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = stringProvider.get(R.string.premium_choose_plan))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(ioDispatcher) { premiumRepository.purchasePlan(selectedPlanId) }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.toUserFriendlyMessage(stringProvider, R.string.premium_error_title),
                )
            }
        }
    }

    private fun restorePurchase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(ioDispatcher) { premiumRepository.restorePurchase() }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message,
            )
        }
    }
}
