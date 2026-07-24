package com.mejoresiagratis.lumiai.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.music.MusicSensitivity
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import com.mejoresiagratis.lumiai.domain.repository.MusicConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del modo Musica. [hasSubscription] gatea el modo de forma ESTRICTA
 * (decision de producto: solo usuarios de pago; el desbloqueo temporal por
 * anuncios NO lo concede, igual que el acento Multicolor).
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicConfigRepository,
    entitlementRepo: EntitlementRepository,
    torch: TorchController
) : ViewModel() {

    val hasFlash: Boolean = torch.hasFlash

    val hasSubscription: StateFlow<Boolean> = entitlementRepo.entitlements
        .map { it.hasSubscription }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val sensitivity: StateFlow<MusicSensitivity> = repository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MusicSensitivity.MEDIA)

    fun setSensitivity(value: MusicSensitivity) =
        viewModelScope.launch { repository.setSensitivity(value) }
}
