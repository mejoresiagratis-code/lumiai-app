package com.mejoresiagratis.lumiai.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableStateFlow(auth.currentUser?.toAuthUser())
    override val currentUser: Flow<AuthUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { fa ->
            _currentUser.value = fa.currentUser?.toAuthUser()
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous,
        isEmailVerified = isEmailVerified
    )

    override val googleWebClientId: String?
        get() {
            val id = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            return if (id != 0) context.getString(id) else null
        }

    override suspend fun ensureAnonymous() {
        if (auth.currentUser != null) return
        runCatching { auth.signInAnonymously().await() }
            .onFailure { Log.w("LumiAuth", "Anonymous sign-in failed: ${it.message}") }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching { auth.signInWithEmailAndPassword(email, password).await(); Unit }.toAuthResult()

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            val cred = EmailAuthProvider.getCredential(email, password)
            val current = auth.currentUser
            if (current != null && current.isAnonymous) {
                current.linkWithCredential(cred).await()
            } else {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            Unit
        }.toAuthResult()

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatching {
            val cred = GoogleAuthProvider.getCredential(idToken, null)
            val current = auth.currentUser
            if (current != null && current.isAnonymous) {
                try {
                    current.linkWithCredential(cred).await()
                } catch (e: FirebaseAuthUserCollisionException) {
                    // Esa cuenta Google ya existe → entrar a ella (se descartan datos anónimos).
                    Log.i("LumiAuth", "Google account exists; signing in instead of linking", e)
                    auth.signInWithCredential(cred).await()
                }
            } else {
                auth.signInWithCredential(cred).await()
            }
            Unit
        }.toAuthResult()

    override suspend fun reloadUser() {
        runCatching { auth.currentUser?.reload()?.await() }
        _currentUser.value = auth.currentUser?.toAuthUser()
    }

    override suspend fun sendEmailVerification(): Result<Unit> =
        runCatching { auth.currentUser?.sendEmailVerification()?.await(); Unit }.toAuthResult()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching { auth.sendPasswordResetEmail(email).await(); Unit }.toAuthResult()

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> =
        runCatching {
            val u = auth.currentUser ?: throw AuthException(AuthError.Unknown)
            val email = u.email ?: throw AuthException(AuthError.Unknown)
            u.reauthenticate(EmailAuthProvider.getCredential(email, password)).await(); Unit
        }.toAuthResult()

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val u = auth.currentUser ?: throw AuthException(AuthError.Unknown)
            u.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await(); Unit
        }.toAuthResult()

    override suspend fun deleteAccount(): Result<Unit> =
        runCatching {
            val u = auth.currentUser ?: throw AuthException(AuthError.Unknown)
            u.delete().await(); Unit
        }.toAuthResult().onSuccess {
            ensureAnonymous()
            _currentUser.value = auth.currentUser?.toAuthUser()
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun <T> Result<T>.toAuthResult(): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it.toAuthException()) })

    private fun Throwable.toAuthException(): Throwable = when (this) {
        is AuthException -> this
        is FirebaseAuthWeakPasswordException -> AuthException(AuthError.WeakPassword)
        is FirebaseAuthUserCollisionException -> AuthException(AuthError.EmailInUse)
        is FirebaseAuthRecentLoginRequiredException -> AuthException(AuthError.RecentLoginRequired)
        is FirebaseAuthInvalidCredentialsException -> AuthException(AuthError.InvalidCredentials)
        is FirebaseNetworkException -> AuthException(AuthError.Network)
        else -> this
    }
}
