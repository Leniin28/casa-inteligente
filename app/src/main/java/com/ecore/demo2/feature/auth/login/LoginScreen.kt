package com.ecore.demo2.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

// ---- Conexión con el ViewModel (normalmente no hace falta tocarla) ----

@Composable
fun LoginRoute(
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onRegister = onRegister,
        onForgotPassword = onForgotPassword,
        onOpenSettings = onOpenSettings,
    )
}

// ---- Diseño (Persona 1) ----

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Casa Inteligente", style = MaterialTheme.typography.headlineMedium)
            Text("Inicia sesión")

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = onLogin, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Entrar")
            }
            TextButton(onClick = onRegister) { Text("Crear cuenta") }
            TextButton(onClick = onForgotPassword) { Text("¿Olvidaste tu contraseña?") }
            TextButton(onClick = onOpenSettings) { Text("Configurar servidor / modo demo") }

            Text(
                "Cuenta de prueba: demo@casa.local / demo1234",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SmartHomeTheme {
        LoginScreen(LoginUiState(email = "demo@casa.local"), {}, {}, {}, {}, {}, {})
    }
}
