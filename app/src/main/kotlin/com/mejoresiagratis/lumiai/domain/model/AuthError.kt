package com.mejoresiagratis.lumiai.domain.model

/** Errores de autenticación de dominio (sin dependencia de Firebase). */
enum class AuthError {
    InvalidCredentials,
    EmailInUse,
    WeakPassword,
    RecentLoginRequired,
    Network,
    // Google Identity Services rechazo la peticion (token nulo o excepcion del
    // flujo de credenciales) — normalmente el SHA-1 del firmante no esta
    // registrado en el proyecto de Firebase. Distinto de Unknown para no
    // confundirlo con un fallo de email/contrasena (QA 13-ago).
    GoogleSignInFailed,
    Unknown
}

/** Excepción portadora de un [AuthError] para propagar por `Result` desde la capa de datos. */
class AuthException(val error: AuthError) : Exception()
