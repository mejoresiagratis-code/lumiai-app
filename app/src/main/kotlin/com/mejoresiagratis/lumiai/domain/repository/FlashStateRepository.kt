package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FlashStateRepository {
    val isOn: StateFlow<Boolean>
    val mode: Flow<FlashMode>
    val settings: Flow<FlashSettings>
    fun setOn(on: Boolean)
    suspend fun setMode(mode: FlashMode)
    suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings)
}
