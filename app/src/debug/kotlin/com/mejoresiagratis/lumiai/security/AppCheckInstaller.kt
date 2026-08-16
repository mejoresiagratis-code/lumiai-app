package com.mejoresiagratis.lumiai.security

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * App Check — variante DEBUG (Q5, 16-ago).
 *
 * En debug NO se puede usar Play Integrity: el APK no viene de Play y la comprobacion falla
 * siempre. Con la aplicacion forzada (enforcement) activada en la consola de Firebase, eso
 * dejaria a las builds de desarrollo sin poder usar Auth ni Firestore.
 *
 * El proveedor de depuracion emite un token local que hay que dar de alta UNA VEZ en la consola
 * de Firebase (App Check -> LumiAI -> Gestionar tokens de depuracion). El token aparece en el
 * Logcat al arrancar, con la etiqueta `DebugAppCheckProvider`, con esta forma:
 *
 *     Enter this debug secret into the allow list in the Firebase Console for your project:
 *     XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
 *
 * Esta clase vive SOLO en el source set de debug: en release no se compila ni se empaqueta.
 */
object AppCheckInstaller {

    fun install() {
        runCatching {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            Log.i(
                "LumiAI-AppCheck",
                "Proveedor de DEPURACION activo. Busca el token en Logcat (DebugAppCheckProvider) " +
                    "y dalo de alta en Firebase Console si activas enforcement."
            )
        }.onFailure { e ->
            Log.w("LumiAI-AppCheck", "No se pudo instalar el proveedor de depuracion", e)
        }
    }
}
