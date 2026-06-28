package com.mejoresiagratis.lumiai.domain.capability

/**
 * Caracteristicas de hardware del dispositivo que la app consulta de forma honesta.
 * Fuente unica para construir [com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities].
 * Nunca se finge una capacidad: si el hardware no esta, el flag es false.
 */
interface DeviceFeatures {
    /** El dispositivo declara tener microfono (PackageManager.FEATURE_MICROPHONE). */
    val hasMicrophone: Boolean
}
