package com.ecore.demo2.feature.auth.forgot

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)
