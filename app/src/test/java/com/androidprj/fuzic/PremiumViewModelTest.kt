package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.ui.screens.premium.PremiumIntent
import com.androidprj.fuzic.ui.screens.premium.PremiumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPlansAndSelectsRecommendedPlan() = runTest {
        val viewModel = PremiumViewModel(FakePremiumRepository(), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(testPremiumPlan), viewModel.uiState.value.plans)
        assertEquals(testPremiumPlan.id, viewModel.uiState.value.selectedPlanId)
    }

    @Test
    fun exposesAlreadyPremiumStatus() = runTest {
        val viewModel = PremiumViewModel(FakePremiumRepository(initialPremium = true), dispatcher, FakeStringProvider)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAlreadyPremium)
    }

    @Test
    fun selectPlanUpdatesSelectedId() = runTest {
        val secondPlan = PremiumPlan("yearly", "Yearly", "$39.99", "Yearly")
        val viewModel = PremiumViewModel(
            FakePremiumRepository(plansResult = Result.success(listOf(testPremiumPlan, secondPlan))),
            dispatcher,
            FakeStringProvider,
        )
        advanceUntilIdle()

        viewModel.onIntent(PremiumIntent.SelectPlan(secondPlan))

        assertEquals("yearly", viewModel.uiState.value.selectedPlanId)
    }

    @Test
    fun purchaseSelectedPlanMarksPremiumAfterSuccess() = runTest {
        val repository = FakePremiumRepository()
        val viewModel = PremiumViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PremiumIntent.Upgrade)
        advanceUntilIdle()

        assertEquals(1, repository.purchaseCalls)
        assertEquals(testPremiumPlan.id, repository.lastPurchasedPlanId)
        assertTrue(viewModel.uiState.value.isAlreadyPremium)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun planLoadingFailureShowsError() = runTest {
        val viewModel = PremiumViewModel(
            FakePremiumRepository(plansResult = Result.failure(IllegalStateException("plans offline"))),
            dispatcher,
            FakeStringProvider,
        )
        advanceUntilIdle()

        assertEquals("plans offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun restoreFailureShowsError() = runTest {
        val repository = FakePremiumRepository().apply {
            restoreResult = Result.failure(IllegalStateException("restore failed"))
        }
        val viewModel = PremiumViewModel(repository, dispatcher, FakeStringProvider)
        advanceUntilIdle()

        viewModel.onIntent(PremiumIntent.RestorePurchase)
        advanceUntilIdle()

        assertEquals(1, repository.restoreCalls)
        assertEquals("restore failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
