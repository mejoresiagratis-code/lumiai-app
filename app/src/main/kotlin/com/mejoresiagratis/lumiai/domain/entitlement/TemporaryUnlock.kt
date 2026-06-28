package com.mejoresiagratis.lumiai.domain.entitlement

/**
 * Desbloqueo Pro temporal (Fase 4: "ver 2 anuncios = 1 h").
 *
 * Modelo puro y testeable: el estado persistido es solo un instante de caducidad
 * (`proUntilMillis`, epoch ms; 0 = sin desbloqueo). La comparación con "ahora" vive
 * aquí como funciones puras para no depender del reloj en los tests. La combinación
 * con [Entitlements] y el gateo de modos se resuelven a nivel de modo en fases
 * posteriores (este modelo no conoce tiers ni cuenta).
 */
object TemporaryUnlock {

    /** Duración estándar concedida por el flujo de anuncios recompensados. */
    const val HOUR_MS: Long = 60L * 60L * 1000L

    /** ¿Hay desbloqueo activo en [nowMillis]? El instante exacto de caducidad NO cuenta como activo. */
    fun isActive(proUntilMillis: Long, nowMillis: Long): Boolean = proUntilMillis > nowMillis

    /** Milisegundos restantes de desbloqueo (nunca negativo). */
    fun remainingMillis(proUntilMillis: Long, nowMillis: Long): Long =
        (proUntilMillis - nowMillis).coerceAtLeast(0L)

    /**
     * Nueva caducidad tras conceder [durationMillis]. Apila desde el más tardío entre
     * "ahora" y la caducidad actual: un desbloqueo vigente se extiende (no se reinicia)
     * y uno ya caducado parte de "ahora".
     */
    fun extended(currentUntilMillis: Long, nowMillis: Long, durationMillis: Long): Long =
        maxOf(nowMillis, currentUntilMillis) + durationMillis
}
