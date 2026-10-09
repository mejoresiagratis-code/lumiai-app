package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.BillingProfileRepository
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Clears personal profile, ad progress and temporary grants; device preferences remain. */
@Singleton
class SessionDataCleaner @Inject constructor(
    private val billingProfile: BillingProfileRepository,
    private val rewardProgress: RewardProgressRepository,
    private val temporaryUnlock: TemporaryUnlockRepository
) {

    /** Attempt all cleanup, but never report success after a failed write. */
    suspend fun clearAll() {
        var failure: Exception? = null
        for (clear in listOf<suspend () -> Unit>(
            { billingProfile.clear() }, { rewardProgress.set(0) }, { temporaryUnlock.clear() }
        )) {
            try { clear() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { if (failure == null) failure = e }
        }
        failure?.let { throw it }
    }
}
