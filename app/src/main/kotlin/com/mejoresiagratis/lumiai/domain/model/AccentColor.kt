package com.mejoresiagratis.lumiai.domain.model

/**
 * Color de acento elegible en Ajustes, en el ORDEN en que se muestran los swatches.
 * Default de la app: [BLUE] (azul vívido, igual que el Splash).
 *
 * Gating (decisión de producto, jul 2026):
 * - [BLUE] y [ORANGE]: libres para todos, incluso sin cuenta.
 * - Resto de sólidos: requieren cuenta (crear cuenta + login).
 * - [MULTICOLOR]: exclusivo de suscriptores Pro (sin puerta por anuncio).
 */
enum class AccentColor(
    /** Requiere cuenta con sesión iniciada (no aplica si además requiere Pro). */
    val requiresAccount: Boolean = false,
    /** Exclusivo de suscripción Pro (el desbloqueo temporal por anuncios NO lo concede). */
    val requiresSubscription: Boolean = false
) {
    BLUE,
    ORANGE,
    AMBER(requiresAccount = true),
    YELLOW(requiresAccount = true),
    GREEN(requiresAccount = true),
    RED(requiresAccount = true),
    VIOLET(requiresAccount = true),
    WHITE(requiresAccount = true),
    MULTICOLOR(requiresSubscription = true);

    /** Si este acento es elegible con el estado dado. Regla pura para poder testearla. */
    fun isUnlocked(hasAccount: Boolean, hasSubscription: Boolean): Boolean = when {
        requiresSubscription -> hasSubscription
        requiresAccount -> hasAccount || hasSubscription
        else -> true
    }
}
