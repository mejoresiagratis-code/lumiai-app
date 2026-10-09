package com.mejoresiagratis.lumiai.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mejoresiagratis.lumiai.BuildConfig
import com.mejoresiagratis.lumiai.domain.entitlement.RecordRewardUseCase
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carga y muestra anuncios recompensados de AdMob. Cada recompensa obtenida registra
 * progreso vía [RecordRewardUseCase] (2 anuncios = 1 h de Pro). El ID de unidad procede
 * de BuildConfig (IDs de prueba en debug, reales en release).
 *
 * Solo solicita anuncios tras [initializeAndPreload] (llamado desde MainActivity cuando UMP
 * permite anuncios): sin esa inicialización, [preload] no hace nada (no se piden anuncios
 * sin consentimiento).
 */
@Singleton
class RewardedAdController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordReward: RecordRewardUseCase,
    private val consent: AdsConsentManager,
    private val auth: com.mejoresiagratis.lumiai.domain.repository.AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rewardedAd: RewardedAd? = null
    private var loading = false
    private var initialized = false
    private var initializing = false
    private var showing = false
    private var generation = 0L
    private var loadedRevision = -1L

    private val _isReady = MutableStateFlow(false)

    init {
        scope.launch {
            consent.state.collect {
                generation++
                rewardedAd = null
                loading = false
                _isReady.value = false
                if (it.allowed && !it.updating) initializeAndPreload()
            }
        }
    }


    /** Emite si hay un anuncio cargado y listo para mostrar. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** Inicializa AdMob una sola vez y precarga un anuncio. Llamar tras obtener consentimiento. */
    fun initializeAndPreload() {
        if (!consent.canRequestAds) return
        if (initialized) {
            preload()
            return
        }
        if (initializing) return
        initializing = true
        MobileAds.initialize(context) {
            scope.launch {
                initialized = true
                initializing = false
                preload()
            }
        }
    }

    /** Precarga un anuncio si AdMob ya está inicializado y no hay uno cargado ni una carga en curso. */
    fun preload() {
        if (!consent.canRequestAds || !initialized || showing || loading || rewardedAd != null) return
        val requestGeneration = generation
        val revision = consent.state.value.revision
        loading = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (requestGeneration != generation || revision != consent.state.value.revision || !consent.canRequestAds) return
                    loadedRevision = revision
                    rewardedAd = ad
                    loading = false
                    _isReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (requestGeneration != generation) return
                    rewardedAd = null
                    loading = false
                    _isReady.value = false
                }
            }
        )
    }

    /**
     * Muestra el anuncio si está listo. Al obtener recompensa, registra progreso y llama a
     * [onReward] con el [RewardProgress.Outcome]. Si no hay anuncio, llama a [onUnavailable]
     * y dispara una precarga.
     */
    fun showIfAvailable(
        activity: Activity,
        onReward: (RewardProgress.Outcome) -> Unit,
        onUnavailable: () -> Unit
    ) {
        val uid = auth.currentUid()
        val ad = rewardedAd
        if (!consent.canRequestAds || showing || uid == null) {
            onUnavailable()
            return
        }
        if (ad == null || loadedRevision != consent.state.value.revision) {
            rewardedAd = null
            _isReady.value = false
            onUnavailable()
            preload()
            return
        }
        rewardedAd = null // Consume before show: a second tap cannot show the same ad.
        showing = true
        val showRevision = consent.state.value.revision
        var credited = false
        _isReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                preload()
            }
        }
        ad.show(activity) { _ ->
            if (!credited && auth.currentUid() == uid && consent.canRequestAds && consent.state.value.revision == showRevision) {
                credited = true
                scope.launch {
                    try {
                        val outcome = recordReward(expectedUid = uid)
                        if (auth.currentUid() == uid) onReward(outcome)
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { onUnavailable() }
                }
            }
        }
    }
}
