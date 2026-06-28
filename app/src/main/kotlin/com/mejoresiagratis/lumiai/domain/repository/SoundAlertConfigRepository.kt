package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.sound.AlertChannel
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import kotlinx.coroutines.flow.Flow

/** Persiste la configuracion del modo Alerta Sonora (categorias activas y sensibilidad). */
interface SoundAlertConfigRepository {
    val config: Flow<SoundAlertConfig>

    suspend fun setEnabled(category: SoundCategory, enabled: Boolean)

    suspend fun setSensitivity(category: SoundCategory, sensitivity: Sensitivity)

    suspend fun setChannel(category: SoundCategory, channel: AlertChannel)

    /** Restablece todas las categorias a sus valores por defecto. */
    suspend fun reset()
}
