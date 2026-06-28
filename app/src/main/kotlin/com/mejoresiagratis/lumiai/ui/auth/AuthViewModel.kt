package com.mejoresiagratis.lumiai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: AuthError? = null,
    val done: Boolean = false,
    val passwordResetSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val webClientId: String? get() = auth.googleWebClientId

    fun signIn(email: String, password: String) =
        launchAuth { auth.signInWithEmail(email.trim(), password) }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            val result = auth.registerWithEmail(email.trim(), password)
            if (result.isSuccess) {
                // Envío de verificación tras registrar (no bloquea la navegación).
                auth.sendEmailVerification()
                _state.value = AuthUiState(done = true)
            } else {
                _state.value = AuthUiState(error = result.toError())
            }
        }
    }

    fun signInWithGoogle(idToken: String) =
        launchAuth { auth.signInWithGoogleIdToken(idToken) }

    fun sendPasswordReset(email: String) {
        val target = email.trim()
        if (target.isBlank()) {
            _state.value = _state.value.copy(error = AuthError.InvalidCredentials)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, passwordResetSent = false)
            val result = auth.sendPasswordReset(target)
            _state.value = if (result.isSuccess) {
                _state.value.copy(loading = false, passwordResetSent = true)
            } else {
                _state.value.copy(loading = false, error = result.toError())
            }
        }
    }

    fun reportFailure() {
        _state.value = AuthUiState(error = AuthError.Unknown)
    }

    private fun launchAuth(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            val result = block()
            _state.value = if (result.isSuccess) {
                AuthUiState(done = true)
            } else {
                AuthUiState(error = result.toError())
            }
        }
    }

    private fun Result<Unit>.toError(): AuthError =
        (exceptionOrNull() as? AuthException)?.error ?: AuthError.Unknown
}
