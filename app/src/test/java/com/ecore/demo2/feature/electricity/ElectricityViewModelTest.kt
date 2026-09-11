package com.ecore.demo2.feature.electricity

import com.ecore.demo2.core.repository.DataResult
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.testutil.FakeSmartHomeRepository
import com.ecore.demo2.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ElectricityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSmartHomeRepository()

    @Test
    fun `shows the reading on success`() {
        val viewModel = ElectricityViewModel(repository)

        assertEquals(LoadState.Success(PreviewData.electricity), viewModel.uiState.value.reading)
    }

    @Test
    fun `flags cached data`() {
        repository.electricity = DataResult(PreviewData.electricity, fromCache = true)

        val state = ElectricityViewModel(repository).uiState.value.reading

        assertTrue((state as LoadState.Success).fromCache)
    }

    @Test
    fun `shows a readable error when the backend is unreachable`() {
        repository.error = IOException("timeout")

        val state = ElectricityViewModel(repository).uiState.value.reading

        assertTrue(state is LoadState.Error)
        assertTrue((state as LoadState.Error).message.contains("conectar"))
    }

    @Test
    fun `retry recovers after an error`() {
        repository.error = IOException()
        val viewModel = ElectricityViewModel(repository)

        repository.error = null
        viewModel.refresh()

        assertTrue(viewModel.uiState.value.reading is LoadState.Success)
    }
}
