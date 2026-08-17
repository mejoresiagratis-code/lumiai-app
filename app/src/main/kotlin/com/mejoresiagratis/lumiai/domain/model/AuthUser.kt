package com.mejoresiagratis.lumiai.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean = false,
    /** Nombre visible del proveedor (Google lo da; email/password suele ser null). */
    val displayName: String? = null,
    /**
     * Foto de perfil del proveedor (17-ago). Google la aporta; con email/contraseña es null
     * y la interfaz cae a la inicial de siempre. Es una URL, no la imagen: quien la pinte
     * decide cómo cargarla.
     */
    val photoUrl: String? = null
)
