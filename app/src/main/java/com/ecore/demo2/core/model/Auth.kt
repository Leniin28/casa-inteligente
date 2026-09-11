package com.ecore.demo2.core.model

data class User(
    val id: String,
    val name: String,
    val email: String,
)

/** Sesión activa. Solo se guarda el token opaco y los datos básicos del usuario, nunca la contraseña. */
data class AuthSession(
    val token: String,
    val user: User,
)
