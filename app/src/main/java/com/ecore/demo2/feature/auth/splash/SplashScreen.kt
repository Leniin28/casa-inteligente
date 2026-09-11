package com.ecore.demo2.feature.auth.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

/** Se muestra mientras se lee la sesión guardada. La redirección la hace SmartHomeApp. */
@Composable
fun SplashScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Casa Inteligente", style = MaterialTheme.typography.headlineMedium)
            CircularProgressIndicator(Modifier.padding(top = 24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SmartHomeTheme { SplashScreen() }
}
