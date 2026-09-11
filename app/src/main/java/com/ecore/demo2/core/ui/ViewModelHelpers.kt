package com.ecore.demo2.core.ui

import com.ecore.demo2.core.repository.DataResult
import com.ecore.demo2.core.util.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Ejecuta una carga del repositorio y la convierte en [LoadState] (Success / Empty / Error). */
suspend fun <T> loadCatching(
    isEmpty: (T) -> Boolean = { false },
    block: suspend () -> DataResult<T>,
): LoadState<T> = try {
    block().toLoadState(isEmpty)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    LoadState.Error(e.toUserMessage())
}

/** En refrescos automáticos, si falla la red se siguen mostrando los datos anteriores. */
fun <T> LoadState<T>.keepDataOnError(previous: LoadState<T>): LoadState<T> =
    if (this is LoadState.Error && previous is LoadState.Success) previous else this

/** Repite [action] cada [intervalMillis] mientras esté iniciado. Se usa desde los ViewModels. */
class AutoRefresher(
    private val scope: CoroutineScope,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val action: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(intervalMillis)
                action()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 5_000L
    }
}
