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
    suspend operator fun invoke(): RewardProgress.Outcome {
        val current = progress.count.first()
        val outcome = RewardProgress.afterReward(current)
        progress.set(outcome.newCount)
        if (outcome.grantsUnlock) unlock.extend(TemporaryUnlock.HOUR_MS)
        return outcome
    }
}
