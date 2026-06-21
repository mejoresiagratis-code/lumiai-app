package com.mejoresiagratis.lumiai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashViewModel @Inject constructor(
    private val repo: FlashStateRepository,
    private val engine: EngineController,
    torch: TorchController
) : ViewModel() {

    val uiState: StateFlow<FlashUiState> =
        combine(repo.isOn, repo.mode, repo.settings) { on, mode, settings ->
            FlashUiState(
                isOn = on,
                mode = mode,
                settings = settings,
                hasFlash = torch.hasFlash,
                maxIntensity = torch.maxIntensityLevel
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlashUiState(hasFlash = torch.hasFlash, maxIntensity = torch.maxIntensityLevel)
        )

    fun toggle() {
        val turningOn = !repo.isOn.value
        repo.setOn(turningOn)
        if (turningOn) engine.start() else engine.stop()
    }

    fun selectMode(mode: FlashMode) {
        viewModelScope.launch { repo.setMode(mode) }
    }

    fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }
}
