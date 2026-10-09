package com.mejoresiagratis.lumiai.ui.home

import com.mejoresiagratis.lumiai.domain.entitlement.AccessState
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

data class FlashUiState(
    val torchFailure: com.mejoresiagratis.lumiai.data.torch.TorchFailure? = null,
    val isOn: Boolean = false,
    val mode: FlashMode = FlashMode.CONTINUOUS,
    val settings: FlashSettings = FlashSettings(),
    val capabilities: DeviceCapabilities = DeviceCapabilities(hasFlash = true, maxTorchLevel = 1),
    val access: AccessState = AccessState()
)
