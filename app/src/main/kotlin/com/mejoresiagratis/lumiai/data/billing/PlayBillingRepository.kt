package com.mejoresiagratis.lumiai.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.mejoresiagratis.lumiai.domain.billing.BillingConnectionState
import com.mejoresiagratis.lumiai.domain.billing.PurchaseOutcome
import com.mejoresiagratis.lumiai.domain.billing.SUBSCRIPTION_BASE_PLAN_ID
import com.mejoresiagratis.lumiai.domain.billing.SUBSCRIPTION_PRODUCT_ID
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionProduct
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Integración con Play Billing Library 9.1.0 (la versión estable más reciente al escribir esto;
 * Google exige v8+ para toda app nueva/actualizada desde el 31 ago 2026). Cliente único de por
 * vida de la app — se conecta una vez y se reconecta solo.
 *
 * Autoridad de gating: [isSubscribed] refleja lo que Google Play confirma que el usuario posee
 * ([BillingClient.queryPurchasesAsync]), NO un campo que la propia app pueda escribir en una base
 * de datos — un usuario no puede "hacerse Pro" editando datos locales o de Firestore.
 *
 * Reconocimiento obligatorio: Play cancela y reembolsa automáticamente cualquier compra no
 * reconocida en 3 días (regla vigente desde PBL v2.0); por eso el procesamiento confirma acknowledge
 * antes de conceder el entitlement.
 */
@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val acknowledgement = PurchaseAcknowledgement()
    private val purchaseMutex = Mutex()
    private var refreshJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _connectionState = MutableStateFlow(BillingConnectionState.CONNECTING)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _product = MutableStateFlow<SubscriptionProduct?>(null)
    override val product: StateFlow<SubscriptionProduct?> = _product.asStateFlow()

    // Callback pendiente del flujo de compra en curso; onPurchasesUpdated lo resuelve.
    private var pendingPurchase: ((PurchaseOutcome) -> Unit)? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        // PBL v8+: la reconexión tras caída del servicio la gestiona la librería sola.
        .enableAutoServiceReconnection()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        _connectionState.value = BillingConnectionState.CONNECTING
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.CONNECTED
                    refresh()
                } else {
                    _connectionState.value = BillingConnectionState.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
                // No reintentamos manualmente: enableAutoServiceReconnection() ya lo hace.
            }
        })
    }

    override fun refresh() {
        if (refreshJob?.isActive == true) return
        // Calling a Billing operation also triggers automatic service reconnection.
        refreshJob = scope.launch {
            try {
                restoreEntitlement()
                queryProduct()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _connectionState.value = BillingConnectionState.UNAVAILABLE
            }
        }
    }

    private suspend fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        val details = result.productDetailsList?.firstOrNull() ?: return
        val offer = details.selectedOffer() ?: return
        val phase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return
        _product.value = SubscriptionProduct(
            productId = details.productId,
            formattedPrice = phase.formattedPrice,
            billingPeriodIso8601 = phase.billingPeriod
        )
    }

    /** Consulta contra Google Play (no contra caché local) qué posee realmente el usuario. */
    private suspend fun restoreEntitlement() = purchaseMutex.withLock {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _connectionState.value = BillingConnectionState.UNAVAILABLE
            return@withLock
        }
        _connectionState.value = BillingConnectionState.CONNECTED
        val purchases = result.purchasesList.filter {
            SUBSCRIPTION_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        var confirmed = false
        for (purchase in purchases) {
            if (acknowledge(purchase)) confirmed = true
        }
        _isSubscribed.value = confirmed
    }

    override suspend fun purchase(activity: Activity): PurchaseOutcome {
        if (pendingPurchase != null) return PurchaseOutcome.Error("")
        val queryResult = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(SUBSCRIPTION_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
        )
        if (queryResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return PurchaseOutcome.Error(
                queryResult.billingResult.debugMessage
            )
        }
        val productDetails = queryResult.productDetailsList?.firstOrNull()
            ?: return PurchaseOutcome.Error("")
        // PBL v9: el offerToken es obligatorio para lanzar el flujo de un producto SUBS.
        val offerToken = productDetails.selectedOffer()?.offerToken
            ?: return PurchaseOutcome.Error("")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        return suspendCancellableCoroutine { cont ->
            val callback: (PurchaseOutcome) -> Unit = { outcome ->
                if (cont.isActive) cont.resume(outcome)
            }
            pendingPurchase = callback
            cont.invokeOnCancellation {
                scope.launch { if (pendingPurchase === callback) pendingPurchase = null }
            }
            val launchResult = client.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchase = null
                if (cont.isActive) {
                    cont.resume(
                        PurchaseOutcome.Error(
                            launchResult.debugMessage.ifBlank { "No se pudo iniciar la compra." },
                            subCodeOf(launchResult)
                        )
                    )
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        // Detach this flow's callback before suspending; do not complete a later purchase.
        val callback = pendingPurchase
        pendingPurchase = null
        scope.launch {
            val outcome = try {
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        val purchase = purchases?.firstOrNull { SUBSCRIPTION_PRODUCT_ID in it.products }
                        when {
                            purchase == null -> PurchaseOutcome.Error("")
                            purchase.purchaseState == Purchase.PurchaseState.PENDING -> PurchaseOutcome.Pending
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED -> purchaseMutex.withLock {
                                if (acknowledge(purchase)) {
                                    _isSubscribed.value = true
                                    PurchaseOutcome.Success
                                } else {
                                    PurchaseOutcome.Error("")
                                }
                            }
                            else -> PurchaseOutcome.Error("")
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.UserCancelled
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        restoreEntitlement()
                        if (_isSubscribed.value) PurchaseOutcome.AlreadyOwned else PurchaseOutcome.Error("")
                    }
                    else -> PurchaseOutcome.Error(result.debugMessage, subCodeOf(result))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                PurchaseOutcome.Error("")
            }
            callback?.invoke(outcome)
        }
    }

    /**
     * Oferta que se compra: la del plan base declarado en [SUBSCRIPTION_BASE_PLAN_ID].
     *
     * Antes se cogia `subscriptionOfferDetails.firstOrNull()` en DOS sitios distintos —el precio
     * mostrado y el flujo de compra— sin garantia de que fueran la misma. Ahora hay un unico
     * punto que decide.
     *
     * Si el plan declarado no aparece se cae a la primera oferta EN LUGAR de fallar: hoy solo
     * existe un plan, y bloquear la compra por un identificador mal escrito seria peor que
     * cobrar el unico precio que hay. En cuanto exista mas de una oferta, esa caida deja de ser
     * aceptable y debe convertirse en error visible (anotado en el roadmap, 22-ago).
     */
    private fun ProductDetails.selectedOffer(): ProductDetails.SubscriptionOfferDetails? {
        val offers = subscriptionOfferDetails ?: return null
        return offers.firstOrNull { it.basePlanId == SUBSCRIPTION_BASE_PLAN_ID }
            ?: offers.firstOrNull()
    }

    private suspend fun acknowledge(purchase: Purchase): Boolean =
        acknowledgement.confirm(purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params).responseCode == BillingClient.BillingResponseCode.OK
        }

    // BillingResult expone un sub-codigo (p. ej. PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS) desde
    // PBL v8 para dar feedback especifico. El nombre exacto del getter varia entre builds del SDK
    // y no se puede verificar en este entorno sin compilar contra la libreria real; en vez de
    // arriesgar un simbolo mal escrito que rompa el build, se busca por reflexion y se degrada a
    // null sin fallar si el metodo no existe con ese nombre exacto.
    private fun subCodeOf(result: BillingResult): Int? = runCatching {
        result.javaClass.methods
            .firstOrNull { it.parameterCount == 0 && it.name.contains("SubResponseCode", ignoreCase = true) }
            ?.invoke(result) as? Int
    }.getOrNull()?.takeIf { it != 0 }
}
