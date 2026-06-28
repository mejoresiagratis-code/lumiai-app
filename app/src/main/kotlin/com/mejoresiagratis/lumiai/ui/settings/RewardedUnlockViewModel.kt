package com.mejoresiagratis.lumiai.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.ads.RewardedAdController
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RewardedUnlockUi(
    val active: Boolean = false,
    val remainingMillis: Long = 0L,
    val adsWatched: Int = 0,
    val adsPerGrant: Int = RewardProgress.ADS_PER_GRANT,
    val adReady: Boolean = false
)

@HiltViewModel
class RewardedUnlockViewModel @Inject constructor(
    temporaryUnlock: TemporaryUnlockRepository,
    rewardProgress: RewardProgressRepository,
    private val rewardedAdController: RewardedAdController
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val ui: StateFlow<RewardedUnlockUi> = combine(
        temporaryUnlock.proUntilMillis,
        rewardProgress.count,
        rewardedAdController.isReady,
        ticker
    ) { proUntil, count, ready, now ->
        RewardedUnlockUi(
            active = TemporaryUnlock.isActive(proUntil, now),
            remainingMillis = TemporaryUnlock.remainingMillis(proUntil, now),
            adsWatched = count,
            adReady = ready
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), RewardedUnlockUi())

    fun watchAd(
        activity: Activity,
        onReward: (RewardProgress.Outcome) -> Unit,
        onUnavailable: () -> Unit
    ) {
        rewardedAdController.showIfAvailable(
            activity = activity,
            onReward = onReward,
            onUnavailable = onUnavailable
        )
    }
}
