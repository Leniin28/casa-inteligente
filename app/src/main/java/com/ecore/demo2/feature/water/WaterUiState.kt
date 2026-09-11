package com.ecore.demo2.feature.water

import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.ui.LoadState

data class WaterUiState(
    val reading: LoadState<WaterReading> = LoadState.Loading,
)
