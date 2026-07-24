package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.music.MusicSensitivity
import kotlinx.coroutines.flow.Flow

/** Preferencias del modo Musica (persistidas). */
interface MusicConfigRepository {
    val sensitivity: Flow<MusicSensitivity>
    suspend fun setSensitivity(value: MusicSensitivity)
}
