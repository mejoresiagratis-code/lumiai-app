package com.mejoresiagratis.lumiai.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean = false,
    /** Nombre visible del proveedor (Google lo da; email/password suele ser null). */
    val displayName: String? = null
)
