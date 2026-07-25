package com.mejoresiagratis.lumiai.domain.repository

/**
 * Foto del usuario a sincronizar contra el registro central (Firestore). Es solo un ESPEJO
 * administrativo para soporte/negocio: [isSubscribed] aqui NO es lo que gatea funciones dentro
 * de la app (eso lo hace [com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository],
 * verificado contra Google Play). Un usuario que edite este documento a mano no gana nada.
 */
data class UserRegistrySnapshot(
    val uid: String,
    val email: String?,
    val fullName: String,
    val billingCountry: String,
    val isSubscribed: Boolean,
    val subscriptionProductId: String?
)

interface UserRegistryRepository {
    suspend fun sync(snapshot: UserRegistrySnapshot)
}
