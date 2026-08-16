package com.mejoresiagratis.lumiai.security

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * App Check — variante RELEASE (Q5, 16-ago).
 *
 * Acredita ante Firebase que las peticiones vienen de una copia legitima de LumiAI instalada
 * desde Play, mediante Play Integrity. Sin esto, cualquiera con el `google-services.json`
 * (que viaja dentro del APK y es trivial de extraer) puede hablar con Auth y Firestore
 * haciendose pasar por la app.
 *
 * DOS DECISIONES DELIBERADAS DE ROBUSTEZ:
 *
 * 1. **Nunca revienta la app.** Play Integrity depende de los Servicios de Google Play: en un
 *    dispositivo sin ellos, con una ROM alternativa o con Play Services corrupto, la
 *    inicializacion puede lanzar. Una linterna que no abre porque falla una comprobacion de
 *    integridad seria un desastre mucho peor que el ataque del que protege. Por eso todo va
 *    envuelto: si falla, App Check queda inactivo y la app funciona con normalidad.
 *
 * 2. **El proveedor de depuracion no existe aqui.** `firebase-appcheck-debug` se declara como
 *    `debugImplementation`, asi que su clase ni siquiera esta en el APK de release. Es mas
 *    solido que un `if (BuildConfig.DEBUG)`: no hay ruta de codigo alguna, ni por reflexion,
 *    que pueda activar el proveedor permisivo en produccion.
 */
object AppCheckInstaller {

    fun install() {
        runCatching {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }.onFailure { e ->
            // Degradado silencioso, pero observable: queda registrado en Crashlytics para
            // poder detectar si esto falla de forma masiva en algun modelo concreto.
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    .recordException(IllegalStateException("App Check (Play Integrity) no instalado", e))
            }
        }
    }
}
