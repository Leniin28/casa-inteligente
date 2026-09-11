package com.ecore.demo2.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecore.demo2.core.ui.LoadState

// Vistas genéricas de estado. El equipo de diseño puede cambiar su aspecto aquí
// y se aplicará a todas las pantallas a la vez.

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
        }
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center)
    }
}

/** Aviso cuando se muestran datos guardados porque no hay conexión. */
@Composable
fun CachedDataNotice(modifier: Modifier = Modifier) {
    Text(
        text = "Sin conexión: mostrando los últimos datos guardados.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Muestra carga / vacío / error automáticamente y llama a [content] solo cuando hay datos. */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No hay datos todavía.",
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingView(modifier)
        LoadState.Empty -> EmptyView(emptyMessage, modifier)
        is LoadState.Error -> ErrorView(state.message, onRetry, modifier)
        is LoadState.Success -> Column(modifier.fillMaxSize()) {
            if (state.fromCache) CachedDataNotice()
            content(state.data)
        }
    }
}
