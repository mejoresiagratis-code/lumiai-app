package com.mejoresiagratis.lumiai.domain.model

/**
 * Configuración persistida del modo Letrero LED (marquesina de texto con estética
 * de panel de puntos). El texto admite emojis (se rasterizan igual que las letras).
 *
 * - [speedLevel]: 1..10, columnas de la rejilla por segundo = nivel x 4.
 * - [scrollLeft]: dirección del desplazamiento (true = hacia la izquierda, lectura natural).
 * - [argb]: color del texto; los emojis conservan su color propio.
 */
data class LedBannerConfig(
    val text: String = "Hello!",
    val argb: Int = DEFAULT_ARGB,
    val speedLevel: Int = DEFAULT_SPEED,
    val scrollLeft: Boolean = true
) {
    fun coerced() = copy(
        text = text.take(MAX_TEXT_LEN),
        speedLevel = speedLevel.coerceIn(MIN_SPEED, MAX_SPEED)
    )

    companion object {
        const val MAX_TEXT_LEN = 120
        const val MIN_SPEED = 1
        const val MAX_SPEED = 10
        const val DEFAULT_SPEED = 5
        /** Verde LED clásico. */
        const val DEFAULT_ARGB: Int = 0xFF2BE04A.toInt()
    }
}
