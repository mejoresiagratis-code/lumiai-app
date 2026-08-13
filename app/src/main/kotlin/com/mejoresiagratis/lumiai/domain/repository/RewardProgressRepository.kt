package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

/** Persiste cuántos anuncios recompensados se han acumulado hacia el próximo desbloqueo. */
interface RewardProgressRepository {
    val count: Flow<Int>
    suspend fun set(value: Int)

    /**
     * Al detectar un versionCode distinto del guardado (actualización de la app), reinicia
     * el contador a 0 y registra el nuevo versionCode. Sin efecto en la instalación inicial.
     * Ver [com.mejoresiagratis.lumiai.domain.entitlement.ProProgressReset] para la regla pura.
     */
    suspend fun resetIfVersionChanged(currentVersionCode: Int)
}
