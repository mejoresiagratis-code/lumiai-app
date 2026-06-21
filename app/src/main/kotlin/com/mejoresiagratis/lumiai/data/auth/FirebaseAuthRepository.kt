package com.mejoresiagratis.lumiai.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.let { AuthUser(it.uid, it.email, it.isAnonymous) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

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
        runCatching { auth.signInWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> =
        runCatching { auth.createUserWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatching {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await(); Unit
        }

    override suspend fun signOut() {
        auth.signOut()
    }
}
