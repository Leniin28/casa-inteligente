package com.ecore.demo2.feature.auth.forgot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(state, viewModel::onEmailChange, viewModel::submit, onBack)
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Recuperar contraseña", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Introduce tu email y te enviaremos instrucciones.")
            OutlinedTextField(
                value = state.email, onValueChange = onEmailChange, label = { Text("Email") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.sent) Text("Si el email está registrado, recibirás las instrucciones.")
            Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Enviar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    SmartHomeTheme { ForgotPasswordScreen(ForgotPasswordUiState(sent = true), {}, {}, {}) }
}
