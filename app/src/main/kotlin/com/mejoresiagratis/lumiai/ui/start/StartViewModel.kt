package com.mejoresiagratis.lumiai.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    onboarding: OnboardingPreferencesRepository,
    private val auth: AuthRepository
) : ViewModel() {

    init {
        // Todos arrancan con sesión anónima (uid estable) hasta crear cuenta.
        viewModelScope.launch { auth.ensureAnonymous() }
    }

    val onboardingCompleted: StateFlow<Boolean?> =
        onboarding.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
