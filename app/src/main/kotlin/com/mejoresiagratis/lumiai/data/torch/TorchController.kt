package com.mejoresiagratis.lumiai.data.torch

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
}
