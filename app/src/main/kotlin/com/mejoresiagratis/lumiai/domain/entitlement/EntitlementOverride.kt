package com.mejoresiagratis.lumiai.domain.entitlement

/**
 * Override de superusuario (solo debug) para forzar permisos sin auth/compra reales.
 *
 * Cada campo es tri-estado: `null` = no forzar (usar el valor real), `true`/`false` = forzar.
 * Modelo puro; solo se aplica en builds debug (ver DefaultEntitlementRepository).
 */
data class EntitlementOverride(
    val forceAccount: Boolean? = null,
    val forceSubscription: Boolean? = null
) {
    /** Aplica el override sobre unos [Entitlements] reales, respetando los campos no forzados. */
    fun apply(base: Entitlements): Entitlements = Entitlements(
        hasAccount = forceAccount ?: base.hasAccount,
        hasSubscription = forceSubscription ?: base.hasSubscription
    )
}
