package com.ecore.demo2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.BuildConfig
import com.ecore.demo2.core.datastore.SettingsRepository
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.ThemeMode
import com.ecore.demo2.core.repository.AssistantRepository
import com.ecore.demo2.core.repository.AuthRepository
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Compartido por Ajustes, Configuración de casa y Notificaciones. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val smartHomeRepository: SmartHomeRepository,
    private val assistantRepository: AssistantRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    val firstLoad = state.settings == null
                    state.copy(
                        settings = settings,
                        backendUrlInput = if (firstLoad) settings.backendUrl else state.backendUrlInput,
                        houseNameInput = if (firstLoad) settings.houseName else state.houseNameInput,
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /**
     * Cambiar entre Demo y API invalida la caché, la conversación y la sesión
     * (el token de una fuente no sirve en la otra), así que se vuelve a Login.
     */
    fun setDataSource(type: DataSourceType) {
        if (type == _uiState.value.settings?.dataSource) return
        viewModelScope.launch {
            settingsRepository.setDataSource(type)
            resetSessionData()
        }
    }

    fun onBackendUrlChange(value: String) = _uiState.update { it.copy(backendUrlInput = value, error = null, message = null) }

    fun saveBackendUrl() = saveUrl(_uiState.value.backendUrlInput)

    fun resetBackendUrl() {
        _uiState.update { it.copy(backendUrlInput = BuildConfig.DEFAULT_BACKEND_URL) }
        saveUrl(BuildConfig.DEFAULT_BACKEND_URL)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun onHouseNameChange(value: String) = _uiState.update { it.copy(houseNameInput = value, error = null, message = null) }

    fun saveHouseName() {
        val name = _uiState.value.houseNameInput.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "El nombre no puede estar vacío.") }
            return
        }
        viewModelScope.launch {
            settingsRepository.setHouseName(name)
            _uiState.update { it.copy(message = "Nombre guardado.", error = null) }
        }
    }

    private fun saveUrl(input: String) {
        val url = Validators.normalizeBaseUrl(input)
        if (url == null) {
            _uiState.update { it.copy(error = "URL no válida. Ejemplo: http://10.0.2.2:8000/", message = null) }
            return
        }
        viewModelScope.launch {
            val changed = url != _uiState.value.settings?.backendUrl
            settingsRepository.setBackendUrl(url)
            _uiState.update { it.copy(backendUrlInput = url, message = "URL guardada.", error = null) }
            // Con otro backend la sesión y la caché anteriores ya no son válidas.
            if (changed && _uiState.value.settings?.dataSource == DataSourceType.API) resetSessionData()
        }
    }

    private suspend fun resetSessionData() {
        smartHomeRepository.clearCache()
        assistantRepository.clear()
        authRepository.clearLocalSession()
    }
}
