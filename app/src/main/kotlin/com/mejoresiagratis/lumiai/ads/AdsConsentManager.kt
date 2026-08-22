package com.mejoresiagratis.lumiai.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el consentimiento de privacidad (UMP de Google). Sin un estado de consentimiento
 * válido (relevante en el EEE) no se solicitan anuncios: la app comprueba [canRequestAds]
 * antes de inicializar AdMob.
 */
@Singleton
class AdsConsentManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /** ¿UMP permite solicitar anuncios con el estado de consentimiento actual? */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * ¿Hay que ofrecer al usuario un acceso VISIBLE para revisar su consentimiento? (22-ago)
     *
     * Google lo EXIGE donde su normativa lo requiere —el EEE, entre otros—: no basta con pedir
     * el consentimiento una vez, el usuario debe poder volver atrás y cambiarlo. Faltaba por
     * completo: se pedía al arrancar y no había forma de revisarlo nunca más.
     *
     * Se consulta en vez de mostrar la opción siempre porque en regiones sin ese requisito el
     * formulario no existe y abrirlo dejaría al usuario ante una pantalla vacía.
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Abre el formulario para que el usuario revise o revoque su consentimiento.
     * [onError] recibe un mensaje solo si el formulario no pudo mostrarse.
     */
    fun showPrivacyOptions(activity: Activity, onError: (String) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { onError(it.message) }
        }
    }

    /**
     * Actualiza el estado de consentimiento y muestra el formulario si es necesario.
     * Invoca [onResult] con el valor de [canRequestAds] al terminar (éxito o error de red).
     */
    fun gatherConsent(activity: Activity, onResult: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onResult(consentInformation.canRequestAds())
                }
            },
            {
                onResult(consentInformation.canRequestAds())
            }
        )
    }
}
