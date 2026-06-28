package com.mejoresiagratis.lumiai.domain.sound

/**
 * Patron de destello distinto por categoria, como pares (encendido, apagado) en milisegundos.
 * El LED es de un solo color: las categorias se distinguen por RITMO, no por color (honestidad).
 * Logica pura y testeable; el actuador la reproduce con el control del LED.
 */
object SoundAlertFlash {

    /** Devuelve la secuencia on/off (ms) para [category]. Longitud par: on, off, on, off, ... */
    fun patternFor(category: SoundCategory): LongArray = when (category) {
        SoundCategory.TIMBRE -> longArrayOf(120, 120, 120, 500)
        SoundCategory.GOLPES_PUERTA -> longArrayOf(220, 160, 220, 500)
        SoundCategory.TELEFONO -> longArrayOf(120, 120, 120, 120, 120, 500)
        SoundCategory.PERRO -> longArrayOf(160, 140, 160, 500)
        SoundCategory.BEBE -> longArrayOf(650, 350)
        SoundCategory.DESPERTADOR -> longArrayOf(110, 110, 110, 110, 110, 450)
        SoundCategory.SIRENA -> longArrayOf(90, 90, 90, 90, 90, 90, 350)
        SoundCategory.ALARMA_HUMO -> longArrayOf(70, 70, 70, 70, 70, 70, 70, 70, 300)
    }
}
