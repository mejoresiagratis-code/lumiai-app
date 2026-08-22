package com.mejoresiagratis.lumiai.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Identificador del único producto de suscripción de LumiAI (configurado en Play Console). */
const val SUBSCRIPTION_PRODUCT_ID = "lumiai_pro_monthly"

/**
 * Plan base que debe usarse al comprar (configurado en Play Console).
 *
 * Antes se tomaba `subscriptionOfferDetails.firstOrNull()`: **la primera oferta que devolviera
 * Google**. Con un único plan da igual, pero el día que se añada un precio introductorio o una
 * promoción, el orden de esa lista no está garantizado y se podría lanzar el flujo con la oferta
 * equivocada — cobrando algo distinto de lo anunciado. Elegir por identificador explícito
 * elimina esa mina antes de pisarla (22-ago).
 *
 * IMPORTANTE: debe COINCIDIR con el ID del plan base en Play Console. Si no coincide, se cae a
 * la primera oferta disponible; ver `selectedOffer()` en PlayBillingRepository.
 */
const val SUBSCRIPTION_BASE_PLAN_ID = "lumiai-pro-mensual"

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
