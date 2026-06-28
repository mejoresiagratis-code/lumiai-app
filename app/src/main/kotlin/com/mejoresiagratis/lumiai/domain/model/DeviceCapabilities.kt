package com.mejoresiagratis.lumiai.domain.model

/** Capacidades del dispositivo. Unica fuente de verdad para gatear UI de modos/ajustes. */
data class DeviceCapabilities(
    val hasFlash: Boolean,
    val maxTorchLevel: Int,
    val hasMicrophone: Boolean = false
) {
    val supportsTorchStrength: Boolean get() = maxTorchLevel > 1
}
