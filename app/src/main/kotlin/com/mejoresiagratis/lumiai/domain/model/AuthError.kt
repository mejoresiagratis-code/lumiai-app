package com.mejoresiagratis.lumiai.domain.model

/** Errores de autenticación de dominio (sin dependencia de Firebase). */
enum class AuthError {
    InvalidCredentials,
    EmailInUse,
    WeakPassword,
    RecentLoginRequired,
    Network,
    Unknown
}

/** Excepción portadora de un [AuthError] para propagar por `Result` desde la capa de datos. */
class AuthException(val error: AuthError) : Exception()
