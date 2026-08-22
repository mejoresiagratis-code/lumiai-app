package com.mejoresiagratis.lumiai

import android.app.Application
import android.os.StrictMode
import com.mejoresiagratis.lumiai.domain.billing.SUBSCRIPTION_PRODUCT_ID
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import com.mejoresiagratis.lumiai.domain.entitlement.SessionDataCleaner
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementOverrideRepository
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.UserRegistrySnapshot
import com.mejoresiagratis.lumiai.domain.repository.UserRegistryRepository
import com.mejoresiagratis.lumiai.security.AppCheckInstaller
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    @Inject lateinit var rewardProgress: RewardProgressRepository
    @Inject lateinit var sessionData: SessionDataCleaner

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        // StrictMode ANTES de super: caza I/O en el hilo principal y fugas de recursos
        // desde el primer frame. Solo debug; en release no existe.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
        super.onCreate()
        // App Check LO PRIMERO tras super.onCreate() (Q5, 16-ago): tiene que quedar instalado
        // antes de que cualquier servicio de Firebase haga su primera peticion, o esa peticion
        // saldria sin token. Las rutinas de abajo (registro de usuario, perfil) hablan con
        // Firestore/Auth, asi que el orden aqui no es cosmetico.
        // La implementacion vive en el source set de cada variante: Play Integrity en release,
        // proveedor de depuracion en debug.
        AppCheckInstaller.install()
        purgeOrphanProfileOnAnonymous()
        purgeGodOverrideOnRelease()
        resetProProgressOnUpdate()
        seedBillingProfileOnSignIn()
        syncUserRegistryOnChange()
    }

    /**
     * Datos personales HUÉRFANOS (17-ago): instalaciones anteriores al arreglo pueden arrastrar
     * nombre y país de una cuenta cerrada, porque el logout no los borraba. Si al arrancar el
     * usuario es anónimo (o no hay ninguno) pero quedan datos de perfil, no son de nadie: se
     * limpian. Sin esto, el defecto seguiría vivo en los móviles ya instalados aunque el código
     * nuevo lo impida hacia delante.
     */
    private fun purgeOrphanProfileOnAnonymous() {
        appScope.launch {
            runCatching {
                val user = auth.currentUser.first()
                if (user == null || user.isAnonymous) {
                    val profile = billingProfileRepo.profile.first()
                    if (profile.fullName.isNotBlank() || profile.billingCountry.isNotBlank()) {
                        sessionData.clearAll()
                    }
                }
            }
        }
    }

    /**
     * Actualización de versión detectada -> reinicia el contador de anuncios hacia la
     * próxima hora de Pro gratuita (decisión de producto, 13-ago). El desbloqueo YA
     * ACTIVO (si el usuario está disfrutando de una hora en curso) no se toca aquí:
     * solo se resetea cuántos anuncios lleva vistos hacia el PRÓXIMO premio.
     */
    private fun resetProProgressOnUpdate() {
        appScope.launch { runCatching { rewardProgress.resetIfVersionChanged(BuildConfig.VERSION_CODE) } }
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
