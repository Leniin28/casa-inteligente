package com.ecore.demo2.feature.devices

import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.ui.LoadState

data class DevicesUiState(
    val devices: LoadState<List<Device>> = LoadState.Loading,
)
