package com.mejoresiagratis.lumiai.domain.flash

/**
 * Regla pura (13-ago): distingue un apagado de la linterna CAUSADO POR NOSOTROS de uno
 * EXTERNO (el boton "Desactivar" de la notificacion de Samsung, u otra app). El callback
 * de Android (`onTorchModeChanged`) no dice QUIEN causo el cambio — solo que ocurrio.
 * Por eso se usa una ventana de tiempo: si nuestro propio codigo llamo a apagar hace
 * menos de [graceMs], el evento es nuestro (se descarta); si no, es externo.
 *
 * Una ventana de tiempo es mas robusta que una bandera booleana con set/clear: el
 * callback del sistema llega de forma ASINCRONA (Binder + Handler) en un hilo distinto
 * al que hizo la llamada, así que una bandera podría limpiarse antes de que el callback
 * llegue a leerla — el margen de tiempo absorbe esa latencia sin ese riesgo de carrera.
 */
object SelfOffWindow {
    fun isOwnOff(lastSelfOffAtMs: Long, nowMs: Long, graceMs: Long = 400L): Boolean =
        (nowMs - lastSelfOffAtMs) <= graceMs
}
