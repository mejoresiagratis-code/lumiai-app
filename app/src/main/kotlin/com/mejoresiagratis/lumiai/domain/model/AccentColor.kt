package com.mejoresiagratis.lumiai.domain.model

/**
 * Color de acento elegible en Ajustes.
 * - [MULTICOLOR]: cada modo usa su propio color (lo aplica la pantalla principal por modo).
 * - El resto: color sólido aplicado a todos los modos por igual.
 * Default de la app: [YELLOW] (amarillo de la linterna del Splash).
 */
enum class AccentColor { MULTICOLOR, YELLOW, AMBER, WHITE, RED, BLUE, GREEN, VIOLET }
