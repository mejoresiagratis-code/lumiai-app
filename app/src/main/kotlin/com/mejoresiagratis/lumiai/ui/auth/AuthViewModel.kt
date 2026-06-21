package com.mejoresiagratis.lumiai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val failed: Boolean = false,
    val done: Boolean = false
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

    fun register(email: String, password: String) =
        launchAuth { auth.registerWithEmail(email.trim(), password) }

    fun signInWithGoogle(idToken: String) =
        launchAuth { auth.signInWithGoogleIdToken(idToken) }

    fun reportFailure() {
        _state.value = AuthUiState(failed = true)
    }

    private fun launchAuth(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            val result = block()
            _state.value =
                if (result.isSuccess) AuthUiState(done = true) else AuthUiState(failed = true)
        }
    }
}
