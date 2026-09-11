package com.ecore.demo2.feature.dashboard

import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.ui.LoadState

data class DashboardUiState(
    val houseName: String = "",
    val summary: LoadState<DashboardSummary> = LoadState.Loading,
)
