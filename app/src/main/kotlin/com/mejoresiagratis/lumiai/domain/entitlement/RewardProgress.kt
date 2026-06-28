package com.mejoresiagratis.lumiai.domain.entitlement

/**
 * Progreso hacia el desbloqueo por anuncios: "ver [ADS_PER_GRANT] anuncios = 1 tramo de Pro".
 *
 * Lógica pura; la persistencia del contador y la concesión efectiva viven fuera
 * (ver RewardProgressRepository y RecordRewardUseCase).
 */
object RewardProgress {

    /** Anuncios recompensados necesarios para conceder un tramo de Pro. */
    const val ADS_PER_GRANT: Int = 2

    /** Resultado de registrar un anuncio recompensado visto. */
    data class Outcome(val newCount: Int, val grantsUnlock: Boolean)

    /**
     * Estado tras ver un anuncio partiendo de [currentCount] (anuncios ya acumulados).
     * Al alcanzar el umbral concede el desbloqueo y reinicia el contador a 0.
     */
    fun afterReward(currentCount: Int): Outcome {
        val n = currentCount.coerceAtLeast(0) + 1
        return if (n >= ADS_PER_GRANT) Outcome(newCount = 0, grantsUnlock = true)
        else Outcome(newCount = n, grantsUnlock = false)
    }
}
