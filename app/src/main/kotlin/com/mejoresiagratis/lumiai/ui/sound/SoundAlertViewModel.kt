package com.mejoresiagratis.lumiai.ui.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertConfigRepository
import com.mejoresiagratis.lumiai.domain.sound.AlertChannel
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del modo Alerta Sonora respaldado por DataStore (persistente entre sesiones).
 * [hasFlash] gatea la oferta de canales de aviso en la UI: sin flash, solo se ofrece pantalla
 * (no se finge una capacidad inexistente).
 */
@HiltViewModel
class SoundAlertViewModel @Inject constructor(
    private val repository: SoundAlertConfigRepository,
    torch: TorchController
) : ViewModel() {

    val hasFlash: Boolean = torch.hasFlash

    val config: StateFlow<SoundAlertConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SoundAlertConfig())

    fun setEnabled(category: SoundCategory, enabled: Boolean) =
        viewModelScope.launch { repository.setEnabled(category, enabled) }

    fun setSensitivity(category: SoundCategory, sensitivity: Sensitivity) =
        viewModelScope.launch { repository.setSensitivity(category, sensitivity) }

    fun setChannel(category: SoundCategory, channel: AlertChannel) =
        viewModelScope.launch { repository.setChannel(category, channel) }

    fun reset() = viewModelScope.launch { repository.reset() }
}
