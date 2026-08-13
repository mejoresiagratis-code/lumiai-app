package com.mejoresiagratis.lumiai.data.torch

import kotlinx.coroutines.flow.Flow

/** Unica abstraccion que controla el LED por hardware. */
interface TorchController {
    val hasFlash: Boolean
    val maxIntensityLevel: Int
    fun turnOn(intensityLevel: Int)
    fun turnOff()

    /**
     * "Apagado" DENTRO de un patron activo (SOS/Estrobo/Baliza/Morse), distinto del
     * apagado real de [turnOff]. EXPERIMENTAL (QA 13-ago): en dispositivos con
     * intensidad variable (API 33+, FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1), la
     * implementacion mantiene la linterna ENCENDIDA al nivel minimo del hardware en
     * vez de apagarla de verdad — Android distingue `onTorchModeChanged` (encendida/
     * apagada) de `onTorchStrengthLevelChanged` (solo intensidad, con la luz ya
     * encendida): al no cruzar nunca el primer callback durante el patron, el
     * indicador del sistema (el de Samsung que parpadea al ritmo del flash) no
     * debería re-dispararse en cada pulso.
     *
     * En dispositivos sin ese soporte, cae a un apagado real identico a [turnOff] —
     * cero cambio de comportamiento ahi. Pendiente de validar en el S26 de Pablo que
     * el nivel minimo se perciba como "apagado" en oscuridad real y no solo atenuado
     * (si no, el contraste del patron se pierde y hay que revertir a [turnOff]).
     */
    fun pulseOff()

    /**
     * Se emite cuando la linterna se apago por una via EXTERNA a esta app — el boton
     * "Desactivar" de la notificacion del sistema de Samsung, u otra app usando la
     * camara — mientras nosotros creiamos que debia seguir encendida (QA 13-ago). Sin
     * esto, el motor de patrones (SOS/Estrobo/Baliza/Morse) seguia reencendiendo la luz
     * en cada pulso sin saber que algo externo la habia apagado, y la notificacion del
     * sistema "revivia" en cada ciclo mientras el boton de nuestra UI quedaba pillado
     * en "encendido". Los apagados PROPIOS (turnOff, y el respaldo interno de pulseOff
     * en hardware sin intensidad variable) quedan filtrados — ver [SelfOffWindow].
     */
    val externalOffEvents: Flow<Unit>
}
