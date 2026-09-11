package com.ecore.demo2.feature.auth.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun ProfileRoute(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state, onLogout = viewModel::logout, onBack = onBack)
}

@Composable
fun ProfileScreen(state: ProfileUiState, onLogout: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = "Perfil", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "Usuario") {
                ValueRow("Nombre", state.user?.name.orEmpty())
                ValueRow("Email", state.user?.email.orEmpty())
            }
            Button(onClick = onLogout, enabled = !state.isLoggingOut, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isLoggingOut) "Cerrando sesión..." else "Cerrar sesión")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    SmartHomeTheme { ProfileScreen(ProfileUiState(user = PreviewData.user), {}, {}) }
}
