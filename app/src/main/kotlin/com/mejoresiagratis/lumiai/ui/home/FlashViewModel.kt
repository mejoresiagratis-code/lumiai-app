package com.mejoresiagratis.lumiai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
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
    entitlementRepo: EntitlementRepository,
    torch: TorchController
) : ViewModel() {

    private val capabilities = DeviceCapabilities(
        hasFlash = torch.hasFlash,
        maxTorchLevel = torch.maxIntensityLevel
    )

    val uiState: StateFlow<FlashUiState> =
        combine(repo.isOn, repo.mode, repo.settings, entitlementRepo.entitlements) { on, mode, settings, ent ->
            FlashUiState(isOn = on, mode = mode, settings = settings, capabilities = capabilities, entitlements = ent)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlashUiState(capabilities = capabilities)
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
