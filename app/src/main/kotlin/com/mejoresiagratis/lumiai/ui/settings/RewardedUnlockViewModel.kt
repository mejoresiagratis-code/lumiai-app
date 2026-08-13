package com.mejoresiagratis.lumiai.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.ads.RewardedAdController
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import com.mejoresiagratis.lumiai.domain.entitlement.Tier
import com.mejoresiagratis.lumiai.domain.entitlement.TemporaryUnlock
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
import javax.inject.Inject

data class RewardedUnlockUi(
    val active: Boolean = false,
    val remainingMillis: Long = 0L,
    val adsWatched: Int = 0,
    val adsPerGrant: Int = RewardProgress.ADS_PER_GRANT,
    val adReady: Boolean = false,
    /** Acceso efectivo al tier IA: suscripción Pro o desbloqueo temporal activo. */
    val proUnlocked: Boolean = false,
    /** Suscripción Pro real (SIN contar el desbloqueo temporal). Gate estricto de Multicolor. */
    val hasSubscription: Boolean = false,
    /** ¿Cuenta con correo verificado? Requisito para ver anuncios (13-ago). */
    val hasAccount: Boolean = false,
    val isEmailVerified: Boolean = false,
    /** Puede ver anuncios para probar Pro: cuenta CON correo verificado (13-ago). */
    val canTryPro: Boolean = false
)

@HiltViewModel
class RewardedUnlockViewModel @Inject constructor(
    temporaryUnlock: TemporaryUnlockRepository,
    rewardProgress: RewardProgressRepository,
    entitlementRepo: EntitlementRepository,
    private val rewardedAdController: RewardedAdController
) : ViewModel() {

    private val entitlements = entitlementRepo.entitlements

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
        entitlements,
        ticker
    ) { proUntil, count, ready, ent, now ->
        val active = TemporaryUnlock.isActive(proUntil, now)
        RewardedUnlockUi(
            active = active,
            remainingMillis = TemporaryUnlock.remainingMillis(proUntil, now),
            adsWatched = count,
            adReady = ready,
            proUnlocked = ent.unlocks(Tier.AI) || active,
            hasSubscription = ent.hasSubscription,
            hasAccount = ent.hasAccount,
            isEmailVerified = ent.isEmailVerified,
            canTryPro = ent.canTryProByAd()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), RewardedUnlockUi())

    fun watchAd(
        activity: Activity,
        onReward: (RewardProgress.Outcome) -> Unit,
        onUnavailable: () -> Unit
    ) {
        // Cinturon de seguridad (13-ago): fuente unica de verdad ademas del gateo en
        // cada pantalla — si por cualquier via se llega aqui sin cuenta verificada,
        // no se muestra el anuncio en vez de dejarlo pasar silenciosamente.
        if (!ui.value.canTryPro) {
            onUnavailable()
            return
        }
        rewardedAdController.showIfAvailable(
            activity = activity,
            onReward = onReward,
            onUnavailable = onUnavailable
        )
    }
}
