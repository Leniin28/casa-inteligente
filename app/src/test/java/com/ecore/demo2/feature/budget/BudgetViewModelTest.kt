package com.ecore.demo2.feature.budget

import com.ecore.demo2.core.ui.dataOrNull
import com.ecore.demo2.testutil.FakeSmartHomeRepository
import com.ecore.demo2.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSmartHomeRepository()
    // lazy: el ViewModel debe crearse después de que la regla instale el dispatcher de test.
    private val viewModel by lazy { BudgetViewModel(repository) }
    private val electricity get() = viewModel.uiState.value.budgets.dataOrNull!!.first { it.id == "electricity" }

    @Test
    fun `invalid limits are rejected`() {
        viewModel.startEditing(electricity)
        viewModel.onLimitInputChange("-3")
        viewModel.saveLimit()

        assertNotNull(viewModel.uiState.value.editError)
        assertTrue(repository.savedLimits.isEmpty())
    }

    @Test
    fun `valid limit is saved and editing finishes`() {
        viewModel.startEditing(electricity)
        viewModel.onLimitInputChange("7,5")
        viewModel.saveLimit()

        assertEquals(listOf("electricity" to 7.5), repository.savedLimits)
        assertNull(viewModel.uiState.value.editingId)
        assertEquals(7.5, electricity.limit, 0.0)
    }
}
