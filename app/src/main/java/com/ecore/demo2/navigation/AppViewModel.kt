package com.ecore.demo2.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.datastore.SettingsRepository
import com.ecore.demo2.core.model.ThemeMode
import com.ecore.demo2.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado global de la app: sesión (para decidir Login vs Inicio) y tema. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null mientras se lee DataStore (se muestra el Splash). */
    val isLoggedIn: StateFlow<Boolean?> = authRepository.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            if (authRepository.session.first() != null) authRepository.validateSession()
        }
    }
}
