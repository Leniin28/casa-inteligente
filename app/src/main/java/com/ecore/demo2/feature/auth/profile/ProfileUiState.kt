package com.ecore.demo2.feature.auth.profile

import com.ecore.demo2.core.model.User

data class ProfileUiState(
    val user: User? = null,
    val isLoggingOut: Boolean = false,
)
