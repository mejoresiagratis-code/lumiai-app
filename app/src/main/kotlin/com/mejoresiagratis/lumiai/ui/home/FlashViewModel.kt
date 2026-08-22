package com.mejoresiagratis.lumiai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.capability.DeviceFeatures
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.entitlement.ProAccessMonitor
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val proAccess: ProAccessMonitor,
    torch: TorchController,
    deviceFeatures: DeviceFeatures
) : ViewModel() {

    private val capabilities = DeviceCapabilities(
        hasFlash = torch.hasFlash,
        maxTorchLevel = torch.maxIntensityLevel,
        hasMicrophone = deviceFeatures.hasMicrophone
    )

    // Acceso efectivo desde la fuente COMPARTIDA (22-ago): esta combinación vivía aquí, es
    // decir, solo en la interfaz — por eso los servicios seguían corriendo tras caducar el Pro.
    // Ahora la regla es una sola para pantallas y servicios; si cambia, cambia para todos.
    private val accessFlow = proAccess.access

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
        // MUSICA es dueña de su propia sesion (MusicFlashService: LED + notificacion).
        // Arrancar tambien TorchService aqui producia DOS notificaciones en ese modo
        // (QA de Pablo, 13-ago). El resto de modos siguen pasando por TorchService.
        if (uiState.value.mode == FlashMode.MUSIC) {
            if (!turningOn) autoOffJob?.cancel()
            return
        }
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
