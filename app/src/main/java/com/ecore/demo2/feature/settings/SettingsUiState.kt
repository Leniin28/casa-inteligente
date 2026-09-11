package com.ecore.demo2.feature.settings

import com.ecore.demo2.core.model.AppSettings

data class SettingsUiState(
    /** null mientras se leen los ajustes de DataStore. */
    val settings: AppSettings? = null,
    val backendUrlInput: String = "",
    val houseNameInput: String = "",
    /** Mensaje de confirmación ("Guardado"). */
    val message: String? = null,
    val error: String? = null,
)
