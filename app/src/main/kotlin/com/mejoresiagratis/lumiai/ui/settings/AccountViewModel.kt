package com.mejoresiagratis.lumiai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.AuthError
import com.mejoresiagratis.lumiai.domain.model.AuthException
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val working: Boolean = false,
    val verificationSent: Boolean = false,
    val needsReauth: Boolean = false,
    val error: AuthError? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    val user: StateFlow<AuthUser?> =
        auth.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val webClientId: String? get() = auth.googleWebClientId

    private val _ui = MutableStateFlow(AccountUiState())
    val ui: StateFlow<AccountUiState> = _ui.asStateFlow()

    /** Refresca isEmailVerified al volver a la pantalla. */
    fun refresh() {
        viewModelScope.launch { auth.reloadUser() }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            auth.ensureAnonymous()
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(working = true, error = null, verificationSent = false)
            val r = auth.sendEmailVerification()
            _ui.value = if (r.isSuccess) {
                _ui.value.copy(working = false, verificationSent = true)
            } else {
                _ui.value.copy(working = false, error = r.toError())
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(working = true, error = null)
            applyDeleteResult(auth.deleteAccount())
        }
    }

    fun reauthPasswordAndDelete(password: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(working = true, error = null)
            val ra = auth.reauthenticateWithPassword(password)
            if (ra.isFailure) {
                _ui.value = _ui.value.copy(working = false, error = ra.toError())
                return@launch
            }
            applyDeleteResult(auth.deleteAccount())
        }
    }

    fun reauthGoogleAndDelete(idToken: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(working = true, error = null)
            val ra = auth.reauthenticateWithGoogle(idToken)
            if (ra.isFailure) {
                _ui.value = _ui.value.copy(working = false, error = ra.toError())
                return@launch
            }
            applyDeleteResult(auth.deleteAccount())
        }
    }

    fun reportReauthFailure() { _ui.value = _ui.value.copy(working = false, error = AuthError.Unknown) }
    fun dismissReauth() { _ui.value = _ui.value.copy(needsReauth = false) }

    private fun applyDeleteResult(r: Result<Unit>) {
        val err = (r.exceptionOrNull() as? AuthException)?.error
        _ui.value = when {
            r.isSuccess -> _ui.value.copy(working = false, needsReauth = false)
            err == AuthError.RecentLoginRequired -> _ui.value.copy(working = false, needsReauth = true)
            else -> _ui.value.copy(working = false, error = err ?: AuthError.Unknown)
        }
    }

    private fun Result<Unit>.toError(): AuthError =
        (exceptionOrNull() as? AuthException)?.error ?: AuthError.Unknown
}
