package com.mejoresiagratis.lumiai.domain.entitlement

/**
 * Acceso efectivo a un [Tier] en un instante dado: combina los permisos permanentes
 * ([Entitlements]: cuenta/suscripción) con el desbloqueo Pro temporal por anuncios.
 *
 * [temporaryProActive] ya incorpora la comparación con el reloj (se calcula aguas arriba
 * con [TemporaryUnlock.isActive]); este modelo es puro y no conoce el tiempo.
 */
data class AccessState(
    val entitlements: Entitlements = Entitlements(),
    val temporaryProActive: Boolean = false
) {
    /** Un tier está desbloqueado si lo conceden los permisos permanentes o el Pro temporal. */
    fun unlocks(tier: Tier): Boolean = entitlements.unlocks(tier) || temporaryProActive
}
