package com.ecore.demo2.feature.alerts

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.dataOrNull

data class AlertsUiState(
    val alerts: LoadState<List<Alert>> = LoadState.Loading,
    /** Error de una acción puntual (p. ej. marcar como leída) sin ocultar la lista. */
    val actionError: String? = null,
) {
    val unreadCount: Int get() = alerts.dataOrNull?.count { !it.read } ?: 0
}
