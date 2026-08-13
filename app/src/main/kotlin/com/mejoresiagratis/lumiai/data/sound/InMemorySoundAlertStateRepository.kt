package com.mejoresiagratis.lumiai.data.sound

import com.mejoresiagratis.lumiai.domain.repository.SoundAlertStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory a proposito (no DataStore): es estado de sesion puro — si el proceso muere,
 * "escuchando" ya no significa nada y debe volver a false por definicion, igual que el
 * desbloqueo temporal de Pro (13-ago). Persistirlo solo añadiría una fuente más de estado
 * obsoleto que sincronizar.
 */
@Singleton
class InMemorySoundAlertStateRepository @Inject constructor() : SoundAlertStateRepository {
    private val _listening = MutableStateFlow(false)
    override val listening: StateFlow<Boolean> = _listening.asStateFlow()
    override fun setListening(value: Boolean) { _listening.value = value }
}
