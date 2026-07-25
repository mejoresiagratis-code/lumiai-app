package com.mejoresiagratis.lumiai.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Identificador del único producto de suscripción de LumiAI (configurado en Play Console). */
const val SUBSCRIPTION_PRODUCT_ID = "lumiai_pro_monthly"

/** Producto de suscripción listo para mostrar (precio ya formateado por Play para la región/moneda del usuario). */
data class SubscriptionProduct(
    val productId: String,
    val formattedPrice: String,
    val billingPeriodIso8601: String
)

/** Estado de conexión con el servicio de Play Billing. */
enum class BillingConnectionState { CONNECTING, CONNECTED, DISCONNECTED, UNAVAILABLE }

/**
 * Resultado de un intento de compra. [Error] incluye el código de sub-respuesta de PBL 9.x
 * cuando existe (p. ej. fondos insuficientes) para dar feedback específico al usuario.
 */
sealed interface PurchaseOutcome {
    data object Success : PurchaseOutcome
    data object UserCancelled : PurchaseOutcome
    data object AlreadyOwned : PurchaseOutcome
    /** La compra quedó pendiente (p. ej. pago en efectivo offline); no hay entitlement aún. */
    data object Pending : PurchaseOutcome
    data class Error(val message: String, val subCode: Int? = null) : PurchaseOutcome
}

/**
 * Fuente de verdad de la suscripción, respaldada por [com.android.billingclient.api.BillingClient].
 * Esta es la autoridad de gating: se consulta contra los servicios de Google Play
 * (queryPurchasesAsync), no contra un campo editable por el cliente en una base de datos propia
 * — eso la hace mucho más difícil de falsificar que un simple booleano en Firestore.
 */
interface SubscriptionRepository {
    val connectionState: StateFlow<BillingConnectionState>
    val isSubscribed: StateFlow<Boolean>
    val product: StateFlow<SubscriptionProduct?>

    /** Refresca precio del producto y estado de propiedad contra Play (llamar en onStart). */
    fun refresh()

    /** Lanza el flujo de compra nativo de Play sobre [activity]. Suspende hasta el resultado. */
    suspend fun purchase(activity: Activity): PurchaseOutcome
}
