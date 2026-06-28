package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

/** Persiste cuántos anuncios recompensados se han acumulado hacia el próximo desbloqueo. */
interface RewardProgressRepository {
    val count: Flow<Int>
    suspend fun set(value: Int)
}
