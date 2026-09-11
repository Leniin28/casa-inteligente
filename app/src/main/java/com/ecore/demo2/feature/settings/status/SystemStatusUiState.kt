package com.ecore.demo2.feature.settings.status

import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.ui.LoadState

data class SystemStatusUiState(
    val status: LoadState<SystemStatus> = LoadState.Loading,
    val backendUrl: String = "",
)
