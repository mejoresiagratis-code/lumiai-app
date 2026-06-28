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
