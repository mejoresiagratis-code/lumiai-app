package com.mejoresiagratis.lumiai.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemePreferencesRepository,
    flashState: FlashStateRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)

    val accentColor: StateFlow<AccentColor> =
        repo.accentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.AMBER)

    /** Modo activo, para que el acento Multicolor siga al modo en vivo. */
    val currentMode: StateFlow<FlashMode> =
        flashState.mode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlashMode.CONTINUOUS)

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { repo.setAccentColor(accent) }
    }
}
