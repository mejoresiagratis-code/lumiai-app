package com.mejoresiagratis.lumiai.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean = false
)
