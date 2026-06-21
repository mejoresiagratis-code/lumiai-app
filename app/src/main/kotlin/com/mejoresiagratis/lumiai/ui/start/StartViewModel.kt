package com.mejoresiagratis.lumiai.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    onboarding: OnboardingPreferencesRepository
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> =
        onboarding.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
