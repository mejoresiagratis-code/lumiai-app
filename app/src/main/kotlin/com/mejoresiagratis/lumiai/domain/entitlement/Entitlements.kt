package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.model.FlashMode

/** Nivel de acceso requerido por un modo. */
enum class Tier { BASIC, ADVANCED, AI }

/**
 * Hoy todos los modos son básicos (gratis). Los avanzados y de IA llegarán en
 * fases posteriores; cuando se añadan, basta con devolver su Tier aquí.
 */
val FlashMode.tier: Tier
    get() = when (this) {
        FlashMode.CONTINUOUS,
        FlashMode.SCREEN,
        FlashMode.SOS_MORSE,
        FlashMode.STROBE,
        FlashMode.BEACON -> Tier.BASIC
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
