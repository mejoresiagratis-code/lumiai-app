package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persiste el desbloqueo Pro temporal concedido por anuncios (Fase 4).
 *
 * [proUntilMillis] es el instante de caducidad en epoch ms (0 = sin desbloqueo).
 * La lógica de "¿activo ahora?" / "¿cuánto queda?" vive en [com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock].
 */
interface TemporaryUnlockRepository {
    val proUntilMillis: Flow<Long>

    /** Concede o extiende el desbloqueo en [durationMillis] (apila sobre el vigente). */
    suspend fun extend(durationMillis: Long)

    /** Elimina cualquier desbloqueo temporal. */
    suspend fun clear()
}
