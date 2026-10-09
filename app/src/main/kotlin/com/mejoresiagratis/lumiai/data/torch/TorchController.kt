package com.mejoresiagratis.lumiai.data.torch

import kotlinx.coroutines.flow.Flow

/** Unica abstraccion que controla el LED por hardware. */
interface TorchController {
    val failure: kotlinx.coroutines.flow.StateFlow<TorchFailure?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)
    val hasFlash: Boolean
    val maxIntensityLevel: Int
    fun turnOn(intensityLevel: Int)
    fun turnOff()

    /** Real LED off between pulses; kept separate to express the pattern boundary. */
    fun pulseOff()

    /**
     * Se emite cuando la linterna se apago por una via EXTERNA a esta app — el boton
     * "Desactivar" de la notificacion del sistema de Samsung, u otra app usando la
     * camara — mientras nosotros creiamos que debia seguir encendida (QA 13-ago). Sin
     * esto, el motor de patrones (SOS/Estrobo/Baliza/Morse) seguia reencendiendo la luz
     * en cada pulso sin saber que algo externo la habia apagado, y la notificacion del
     * sistema "revivia" en cada ciclo mientras el boton de nuestra UI quedaba pillado
     * en "encendido". Los apagados PROPIOS (turnOff y pulseOff) quedan filtrados — ver [SelfOffWindow].
     */
    val externalOffEvents: Flow<Unit>
}
