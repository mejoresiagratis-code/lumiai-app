package com.mejoresiagratis.lumiai.domain.entitlement

/**
 * Regla pura de producto (13-ago): al detectar que la app se actualizo de version, el
 * contador de anuncios hacia la prueba gratuita de Pro se reinicia a 0 — la prueba se
 * "refresca" en cada version nueva. NO toca el desbloqueo temporal ya activo (la hora
 * que el usuario pueda estar disfrutando ahora mismo sigue corriendo igual).
 */
object ProProgressReset {

    /**
     * @param lastVersionCode el versionCode guardado en el arranque anterior, o null si
     *   es la instalacion inicial (nada que reiniciar).
     * @param currentVersionCode el versionCode de este arranque (BuildConfig.VERSION_CODE).
     */
    fun shouldReset(lastVersionCode: Int?, currentVersionCode: Int): Boolean =
        lastVersionCode != null && lastVersionCode != currentVersionCode
}
