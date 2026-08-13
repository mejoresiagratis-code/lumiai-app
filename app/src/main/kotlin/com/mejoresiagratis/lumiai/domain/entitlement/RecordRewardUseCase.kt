package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Registra un anuncio recompensado visto: avanza el contador y, al alcanzar el umbral,
 * concede [TemporaryUnlock.HOUR_MS] de Pro y reinicia. Devuelve el [RewardProgress.Outcome].
 */
class RecordRewardUseCase @Inject constructor(
    private val progress: RewardProgressRepository,
    private val unlock: TemporaryUnlockRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): RewardProgress.Outcome {
        val current = progress.count.first()
        // Regla de producto (QA 13-ago): con el Pro temporal ACTIVO los anuncios no cuentan
        // ni extienden — el maximo canjeable es 1 hora. Sin esto, el usuario podia ver
        // anuncios durante su hora activa y encadenar horas infinitas.
        if (TemporaryUnlock.isActive(unlock.proUntilMillis.first(), now)) {
            return RewardProgress.Outcome(newCount = current, grantsUnlock = false)
        }
        val outcome = RewardProgress.afterReward(current)
        progress.set(outcome.newCount)
        if (outcome.grantsUnlock) unlock.extend(TemporaryUnlock.HOUR_MS)
        return outcome
    }
}
