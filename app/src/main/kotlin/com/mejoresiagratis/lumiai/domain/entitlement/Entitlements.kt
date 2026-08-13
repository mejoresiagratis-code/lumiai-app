package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.FlashMode

/** Nivel de acceso requerido por un modo. */
/**
 * Niveles de acceso:
 * - [BASIC]: libre para todos. [ADVANCED]: cuenta O desbloqueo temporal.
 * - [AI]: suscripcion O desbloqueo temporal (Alerta Sonora).
 * - [PRO]: SOLO suscripcion; el desbloqueo temporal por anuncios NO lo concede
 *   (Musica, como el acento Multicolor).
 */
enum class Tier { BASIC, ADVANCED, AI, PRO }

/**
 * Regla de producto: para INICIAR una compra de suscripcion hace falta tener cuenta con
 * sesion iniciada (a diferencia del desbloqueo temporal por anuncios, que no la exige).
 * Punto de entrada unico para el flujo de Play Billing: la UI solo debe llamar a esto antes
 * de lanzar el flujo de cobro, nunca duplicar la condicion inline.
 */
fun canStartSubscriptionPurchase(hasAccount: Boolean): Boolean = hasAccount

/**
 * Matriz de acceso por modo (decisión de producto, revisada 13-ago-2026):
 * - BASIC (libre para todos, incluso sin cuenta): Continuo y Pantalla.
 * - ADVANCED (SOLO cuenta; el pop-up de estos modos ya NO ofrece desbloqueo por
 *   anuncio — decisión 13-ago para no gastar impresiones en modos ligeros):
 *   SOS, Estrobo, Baliza y Morse. El desbloqueo temporal ganado en otra pantalla
 *   (Música/Alerta Sonora/LED) sigue contando para ellos vía [AccessState] — si
 *   estuviera activo el modo no llegaría bloqueado en primer lugar.
 * - AI (suscripción Pro O desbloqueo temporal por anuncios; invitado: se antepone
 *   el alta de cuenta sin cerrar la puerta del anuncio): Música, Alerta Sonora y
 *   Letrero LED. Música se unifica aquí (antes Tier.PRO estricto, sin anuncio);
 *   Alerta Sonora y LED viven fuera de este enum (no están en el carrusel), su
 *   gate usa Tier.AI vía RewardedUnlockViewModel igualmente.
 * - PRO (estrictamente suscripción, el temporal NO lo abre): reservado para
 *   futuros modos que quieran ese trato; ningún modo actual lo usa.
 */
val FlashMode.tier: Tier
    get() = when (this) {
        FlashMode.CONTINUOUS,
        FlashMode.SCREEN -> Tier.BASIC
        FlashMode.SOS_MORSE,
        FlashMode.STROBE,
        FlashMode.BEACON,
        FlashMode.TEXT_MORSE -> Tier.ADVANCED
        FlashMode.MUSIC -> Tier.AI
    }

/**
 * Permisos efectivos del usuario. Fuente única de verdad para decidir qué
 * desbloquear. Los desbloqueos temporales por anuncios (Fase 4) se combinan
 * a nivel de modo, fuera de este modelo puro.
 */
data class Entitlements(
    val hasAccount: Boolean = false,
    val hasSubscription: Boolean = false,
    val isEmailVerified: Boolean = false
) {
    /**
     * ¿Puede probar Pro viendo anuncios? (regla de producto, 13-ago): hace falta
     * cuenta CON el correo verificado — cierra la puerta que antes dejaba a un
     * invitado ver anuncios sin registrarse en Musica/Alerta Sonora/LED.
     */
    fun canTryProByAd(): Boolean = hasAccount && isEmailVerified

    fun unlocks(tier: Tier): Boolean = when (tier) {
        Tier.BASIC -> true
        Tier.ADVANCED -> hasAccount || hasSubscription
        Tier.AI -> hasSubscription
        Tier.PRO -> hasSubscription
    }
}
