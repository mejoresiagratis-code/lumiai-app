package com.mejoresiagratis.lumiai.ui.home

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

data class FlashUiState(
    val isOn: Boolean = false,
    val mode: FlashMode = FlashMode.CONTINUOUS,
    val settings: FlashSettings = FlashSettings(),
    val hasFlash: Boolean = true,
    val maxIntensity: Int = 1
)
