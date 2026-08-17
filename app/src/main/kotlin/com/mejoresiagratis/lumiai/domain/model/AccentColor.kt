package com.mejoresiagratis.lumiai.domain.model

/**
 * Color de acento elegible en Ajustes, en el ORDEN en que se muestran los swatches.
 * Default de la app: [BLUE] (azul vívido, igual que el Splash).
 *
 * Gating (decisión de producto, jul 2026; MULTICOLOR revisado el 17-ago-2026):
 * - [BLUE] y [ORANGE]: libres para todos, incluso sin cuenta.
 * - Resto de sólidos: requieren cuenta (crear cuenta + login).
 * - [MULTICOLOR]: requiere acceso Pro, y ese acceso puede venir TANTO de la suscripción
 *   COMO del desbloqueo temporal por anuncios — igual que el Letrero LED o la Alerta
 *   sonora. Esto REVIERTE la regla anterior, que lo reservaba a suscriptores de pago:
 *   Pablo decidió que multicolor se comporte como el resto de herramientas Pro.
 */
enum class AccentColor(
    /** Requiere cuenta con sesión iniciada (no aplica si además requiere Pro). */
    val requiresAccount: Boolean = false,
    /** Requiere acceso Pro: suscripción O desbloqueo temporal por anuncios (17-ago). */
    val requiresPro: Boolean = false
) {
    BLUE,
    ORANGE,
    AMBER(requiresAccount = true),
    YELLOW(requiresAccount = true),
    GREEN(requiresAccount = true),
    RED(requiresAccount = true),
    VIOLET(requiresAccount = true),
    WHITE(requiresAccount = true),
    MULTICOLOR(requiresPro = true);

    /**
     * Si este acento es elegible con el estado dado. Regla pura para poder testearla.
     *
     * [hasPro] es el acceso Pro EFECTIVO: suscripción activa o desbloqueo temporal por
     * anuncios en curso (que a su vez exige cuenta con correo verificado).
     */
    fun isUnlocked(hasAccount: Boolean, hasPro: Boolean): Boolean = when {
        requiresPro -> hasPro
        requiresAccount -> hasAccount || hasPro
        else -> true
    }
}
