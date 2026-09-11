package com.ecore.demo2.feature.electricity

import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.ui.LoadState

data class ElectricityUiState(
    val reading: LoadState<ElectricalReading> = LoadState.Loading,
)
