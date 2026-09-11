package com.ecore.demo2.feature.history

import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.ui.LoadState

data class HistoryTotals(
    val electricityKwh: Double,
    val waterLiters: Double,
    val estimatedCost: Double?,
)

data class HistoryUiState(
    val period: HistoryPeriod = HistoryPeriod.WEEK,
    val records: LoadState<List<HistoryRecord>> = LoadState.Loading,
    val totals: HistoryTotals? = null,
)
