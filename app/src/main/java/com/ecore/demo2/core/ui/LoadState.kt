package com.ecore.demo2.core.ui

import com.ecore.demo2.core.repository.DataResult

/**
 * Estado genérico de una carga de datos. Todas las pantallas de datos lo usan:
 * Loading -> Success / Empty / Error.
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Empty : LoadState<Nothing>
    data class Success<T>(val data: T, val fromCache: Boolean = false) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

val <T> LoadState<T>.dataOrNull: T?
    get() = (this as? LoadState.Success)?.data

fun <T> DataResult<T>.toLoadState(isEmpty: (T) -> Boolean = { false }): LoadState<T> =
    if (isEmpty(data)) LoadState.Empty else LoadState.Success(data, fromCache)
