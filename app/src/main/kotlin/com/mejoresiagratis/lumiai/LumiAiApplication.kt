package com.mejoresiagratis.lumiai

import android.app.Application
import com.mejoresiagratis.lumiai.domain.billing.SUBSCRIPTION_PRODUCT_ID
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementOverrideRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistrySnapshot
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class LumiAiApplication : Application() {

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var billingProfileRepo: BillingProfileRepository
    @Inject lateinit var subscriptionRepo: SubscriptionRepository
    @Inject lateinit var userRegistry: UserRegistryRepository
    @Inject lateinit var entitlementOverrideRepo: EntitlementOverrideRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        purgeGodOverrideOnRelease()
        seedBillingProfileOnSignIn()
        syncUserRegistryOnChange()
    }

    /**
     * El override de superusuario (God) es EXCLUSIVO de debug. `DefaultEntitlementRepository` ya
     * lo ignora en release, pero las claves podrían seguir escritas en DataStore de una instalación
     * debug previa (mismo applicationId). Aquí las borramos al arrancar un build release para que
     * no quede rastro de permisos forzados en el dispositivo del usuario.
     */
    private fun purgeGodOverrideOnRelease() {
        if (BuildConfig.DEBUG) return
        appScope.launch { runCatching { entitlementOverrideRepo.clear() } }
    }

    /**
     * Al iniciar sesion con una cuenta real, siembra el perfil de facturacion con datos ya
     * conocidos, SOLO si los campos estan vacios (nunca pisa lo que el usuario escribio):
     *  - nombre completo: el displayName del proveedor (Google lo aporta).
     *  - pais: el del Locale del dispositivo como valor de partida razonable.
     * Los repos hacen el "if empty" de forma atomica; aqui solo disparamos.
     */
    private fun seedBillingProfileOnSignIn() {
        appScope.launch {
            auth.currentUser
                .distinctUntilChanged()
                .collect { user ->
                    if (user == null || user.isAnonymous) return@collect
                    user.displayName?.let { name ->
                        runCatching { billingProfileRepo.prefillFullNameIfEmpty(name) }
                    }
                    val country = Locale.getDefault().displayCountry
                    if (country.isNotBlank()) {
                        runCatching { billingProfileRepo.prefillCountryIfEmpty(country) }
                    }
                }
        }
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
