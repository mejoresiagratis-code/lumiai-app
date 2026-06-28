package com.mejoresiagratis.lumiai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.entitlement.AccessState
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashViewModel @Inject constructor(
    private val repo: FlashStateRepository,
    private val engine: EngineController,
    entitlementRepo: EntitlementRepository,
    temporaryUnlock: TemporaryUnlockRepository,
    torch: TorchController
) : ViewModel() {

    private val capabilities = DeviceCapabilities(
        hasFlash = torch.hasFlash,
        maxTorchLevel = torch.maxIntensityLevel
    )

    // Ticker de 1 s: reevalúa la caducidad del Pro temporal para re-bloquear en vivo.
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    // Acceso efectivo: permisos permanentes combinados con el Pro temporal activo ahora.
    private val accessFlow = combine(
        entitlementRepo.entitlements,
        temporaryUnlock.proUntilMillis,
        ticker
    ) { ent, proUntil, now ->
        AccessState(entitlements = ent, temporaryProActive = TemporaryUnlock.isActive(proUntil, now))
    }

    val uiState: StateFlow<FlashUiState> =
        combine(repo.isOn, repo.mode, repo.settings, accessFlow) { on, mode, settings, access ->
            FlashUiState(isOn = on, mode = mode, settings = settings, capabilities = capabilities, access = access)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlashUiState(capabilities = capabilities)
        )

    private var autoOffJob: Job? = null

    fun toggle() {
        val turningOn = !repo.isOn.value
        repo.setOn(turningOn)
        if (turningOn) {
            engine.start()
            scheduleBeaconAutoOff()
        } else {
            engine.stop()
            autoOffJob?.cancel()
        }
    }

    /** En Baliza, apaga la luz automáticamente tras los minutos elegidos (0 = desactivado). */
    private fun scheduleBeaconAutoOff() {
        autoOffJob?.cancel()
        val state = uiState.value
        val minutes = state.settings.beaconAutoOffMin
        if (state.mode == FlashMode.BEACON && minutes > 0) {
            autoOffJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                if (repo.isOn.value) {
                    repo.setOn(false)
                    engine.stop()
                }
            }
        }
    }

    fun selectMode(mode: FlashMode) {
        viewModelScope.launch { repo.setMode(mode) }
    }

    fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }
}
