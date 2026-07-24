package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.FlashMode

/** Nivel de acceso requerido por un modo. */
enum class Tier { BASIC, ADVANCED, AI }

/**
 * Matriz de acceso por modo (decisión de producto, jul 2026):
 * - BASIC (libre para todos, incluso sin cuenta): Continuo y Pantalla.
 * - ADVANCED (cuenta O anuncio de desbloqueo temporal): SOS, Estrobo, Baliza y Morse.
 * - AI (suscripción Pro O desbloqueo temporal): Alerta Sonora — vive fuera de este
 *   enum (no está en el carrusel); su gate usa Tier.AI vía RewardedUnlockViewModel.
 */
val FlashMode.tier: Tier
    get() = when (this) {
        FlashMode.CONTINUOUS,
        FlashMode.SCREEN -> Tier.BASIC
        FlashMode.SOS_MORSE,
        FlashMode.STROBE,
        FlashMode.BEACON,
        FlashMode.TEXT_MORSE -> Tier.ADVANCED
    }

/**
 * Permisos efectivos del usuario. Fuente única de verdad para decidir qué
 * desbloquear. Los desbloqueos temporales por anuncios (Fase 4) se combinan
 * a nivel de modo, fuera de este modelo puro.
 */
data class Entitlements(
    val hasAccount: Boolean = false,
    val hasSubscription: Boolean = false
) {
    fun unlocks(tier: Tier): Boolean = when (tier) {
        Tier.BASIC -> true
        Tier.ADVANCED -> hasAccount || hasSubscription
        Tier.AI -> hasSubscription
    }
}
