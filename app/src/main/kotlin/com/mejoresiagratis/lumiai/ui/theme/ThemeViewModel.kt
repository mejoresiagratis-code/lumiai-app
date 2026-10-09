package com.mejoresiagratis.lumiai.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import com.mejoresiagratis.lumiai.domain.entitlement.ProAccessMonitor
import com.mejoresiagratis.lumiai.domain.entitlement.Tier
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemePreferencesRepository,
    flashState: FlashStateRepository,
    proAccess: ProAccessMonitor
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val accentColor: StateFlow<AccentColor> =
        combine(repo.accentColor, proAccess.access) { selected, access ->
            if (selected.isUnlocked(access.entitlements.hasAccount, access.unlocks(Tier.AI))) selected
            else {
                try { repo.resetAccentIfMatches(selected) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { /* Still render a permitted color if persistence fails. */ }
                AccentColor.BLUE
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.BLUE)

    val accentStyle: StateFlow<AccentStyle> =
        repo.accentStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentStyle.VIVID)

    val reduceMotion: StateFlow<Boolean> =
        repo.reduceMotion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val highContrast: StateFlow<Boolean> =
        repo.highContrast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val haptics: StateFlow<Boolean> =
        repo.haptics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val autoLockScreen: StateFlow<Boolean> =
        repo.autoLockScreen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Modo activo, para que el acento Multicolor siga al modo en vivo. */
    val currentMode: StateFlow<FlashMode> =
        flashState.mode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlashMode.CONTINUOUS)

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { repo.setAccentColor(accent) }
    }

    fun setAccentStyle(style: AccentStyle) {
        viewModelScope.launch { repo.setAccentStyle(style) }
    }

    fun setReduceMotion(value: Boolean) {
        viewModelScope.launch { repo.setReduceMotion(value) }
    }

    fun setHighContrast(value: Boolean) {
        viewModelScope.launch { repo.setHighContrast(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { repo.setHaptics(value) }
    }

    fun setAutoLockScreen(value: Boolean) {
        viewModelScope.launch { repo.setAutoLockScreen(value) }
    }
}
