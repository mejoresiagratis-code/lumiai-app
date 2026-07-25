package com.mejoresiagratis.lumiai

import android.app.Application
import com.mejoresiagratis.lumiai.domain.billing.SUBSCRIPTION_PRODUCT_ID
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistrySnapshot
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LumiAiApplication : Application() {

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var billingProfileRepo: BillingProfileRepository
    @Inject lateinit var subscriptionRepo: SubscriptionRepository
    @Inject lateinit var userRegistry: UserRegistryRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        syncUserRegistryOnChange()
    }

    /**
     * Mantiene el registro central de usuarios (Firestore) al dia: se sincroniza cada vez que
     * cambia la cuenta con sesion iniciada, el perfil de facturacion o el estado real de la
     * suscripcion (verificado por Play Billing). Los usuarios anonimos NUNCA se registran aqui:
     * solo cuentas reales con las que se les pueda contactar o facturar.
     */
    private fun syncUserRegistryOnChange() {
        appScope.launch {
            combine(
                auth.currentUser,
                billingProfileRepo.profile,
                subscriptionRepo.isSubscribed
            ) { user, profile, subscribed ->
                if (user == null || user.isAnonymous) null
                else UserRegistrySnapshot(
                    uid = user.uid,
                    email = user.email,
                    fullName = profile.fullName,
                    billingCountry = profile.billingCountry,
                    isSubscribed = subscribed,
                    subscriptionProductId = if (subscribed) {
                        SUBSCRIPTION_PRODUCT_ID
                    } else {
                        null
                    }
                )
            }
                .distinctUntilChanged()
                .collect { snapshot -> snapshot?.let { runCatching { userRegistry.sync(it) } } }
        }
    }
}
