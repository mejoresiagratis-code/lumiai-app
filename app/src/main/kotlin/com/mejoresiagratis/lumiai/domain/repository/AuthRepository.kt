package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Usuario actual, o null si no hay sesión Firebase. */
    val currentUser: Flow<AuthUser?>

    /** ID de cliente web (OAuth) para Google Sign-In; null si el proyecto no lo tiene. */
    val googleWebClientId: String?

    suspend fun ensureAnonymous()
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun signOut()
}
