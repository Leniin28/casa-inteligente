package com.ecore.demo2.feature.history

import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.testutil.FakeSmartHomeRepository
import com.ecore.demo2.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSmartHomeRepository()

    @Test
    fun `loads the week by default and computes totals`() {
        repository.history = listOf(
            HistoryRecord(Instant.EPOCH, electricityKwh = 0.2, waterLiters = 100.0, estimatedCost = 0.5),
            HistoryRecord(Instant.EPOCH.plusSeconds(86_400), electricityKwh = 0.3, waterLiters = 50.0, estimatedCost = 0.25),
        )

        val state = HistoryViewModel(repository).uiState.value

        assertEquals(HistoryPeriod.WEEK, state.period)
        assertEquals(listOf(HistoryPeriod.WEEK), repository.requestedPeriods)
        assertEquals(0.5, state.totals!!.electricityKwh, 1e-9)
        assertEquals(150.0, state.totals.waterLiters, 1e-9)
        assertEquals(0.75, state.totals.estimatedCost!!, 1e-9)
    }

    @Test
    fun `changing the filter reloads that period`() {
        val viewModel = HistoryViewModel(repository)

        viewModel.selectPeriod(HistoryPeriod.DAY)

        assertEquals(HistoryPeriod.DAY, viewModel.uiState.value.period)
        assertEquals(HistoryPeriod.DAY, repository.requestedPeriods.last())
    }

    @Test
    fun `empty history shows the empty state`() {
        repository.history = emptyList()

        val state = HistoryViewModel(repository).uiState.value

        assertEquals(LoadState.Empty, state.records)
        assertNull(state.totals)
    }

    @Test
    fun `errors are exposed as error state`() {
        repository.error = IOException()

        assertTrue(HistoryViewModel(repository).uiState.value.records is LoadState.Error)
    }
}
