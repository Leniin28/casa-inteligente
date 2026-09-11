package com.ecore.demo2.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect

/**
 * Llama a [onStart] cuando la pantalla es visible y a [onStop] cuando deja de serlo.
 * Sirve para que el ViewModel refresque datos en vivo solo mientras se ven.
 */
@Composable
fun AutoRefreshEffect(onStart: () -> Unit, onStop: () -> Unit) {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)
    LifecycleStartEffect(Unit) {
        currentOnStart()
        onStopOrDispose { currentOnStop() }
    }
}
