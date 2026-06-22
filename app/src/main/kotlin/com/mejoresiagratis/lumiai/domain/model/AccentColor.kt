package com.mejoresiagratis.lumiai.domain.model

/**
 * Color de acento elegible en Ajustes.
 * - [MULTICOLOR]: cada modo usa su propio color (lo aplica la pantalla principal por modo).
 * - El resto: color sólido aplicado a todos los modos por igual.
 * Default de la app: [AMBER].
 */
enum class AccentColor { MULTICOLOR, AMBER, WHITE, RED, BLUE, GREEN, VIOLET }
