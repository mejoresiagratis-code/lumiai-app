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

    /** Registra; si la sesión es anónima, **vincula** (conserva el uid) en vez de crear. */
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>

    /** Google: si la sesión es anónima, **vincula**; si la cuenta ya existe, entra a ella. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    suspend fun signOut()

    /** Refresca el usuario (p. ej. para reflejar `isEmailVerified` tras verificar). */
    suspend fun reloadUser()
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun reauthenticateWithPassword(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>

    /** Borra la cuenta (RGPD) y vuelve a sesión anónima para que la app siga usable. */
    suspend fun deleteAccount(): Result<Unit>
}
