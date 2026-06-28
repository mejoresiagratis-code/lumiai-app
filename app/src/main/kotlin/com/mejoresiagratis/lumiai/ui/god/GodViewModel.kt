package com.mejoresiagratis.lumiai.ui.god

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.EntitlementOverrideRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GodUi(
    val proRemainingMillis: Long = 0L,
    val proActive: Boolean = false,
    val adsWatched: Int = 0,
    val adsPerGrant: Int = RewardProgress.ADS_PER_GRANT,
    val effectiveAccount: Boolean = false,
    val effectiveSubscription: Boolean = false,
    val forceAccount: Boolean? = null,
    val forceSubscription: Boolean? = null
)

@HiltViewModel
class GodViewModel @Inject constructor(
    private val temporaryUnlock: TemporaryUnlockRepository,
    private val rewardProgress: RewardProgressRepository,
    private val overrideRepo: EntitlementOverrideRepository,
    entitlementRepo: EntitlementRepository
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val ui: StateFlow<GodUi> = combine(
        entitlementRepo.entitlements,
        temporaryUnlock.proUntilMillis,
        rewardProgress.count,
        overrideRepo.override,
        ticker
    ) { ent, proUntil, count, ov, now ->
        GodUi(
            proRemainingMillis = TemporaryUnlock.remainingMillis(proUntil, now),
            proActive = TemporaryUnlock.isActive(proUntil, now),
            adsWatched = count,
            effectiveAccount = ent.hasAccount,
            effectiveSubscription = ent.hasSubscription,
            forceAccount = ov.forceAccount,
            forceSubscription = ov.forceSubscription
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GodUi())

    fun grantMillis(durationMillis: Long) = viewModelScope.launch { temporaryUnlock.extend(durationMillis) }
    fun clearPro() = viewModelScope.launch { temporaryUnlock.clear() }
    fun setRewardCount(value: Int) = viewModelScope.launch { rewardProgress.set(value) }
    fun setForceAccount(value: Boolean?) = viewModelScope.launch { overrideRepo.setForceAccount(value) }
    fun setForceSubscription(value: Boolean?) = viewModelScope.launch { overrideRepo.setForceSubscription(value) }

    fun resetAll() = viewModelScope.launch {
        temporaryUnlock.clear()
        rewardProgress.set(0)
        overrideRepo.clear()
    }
}
