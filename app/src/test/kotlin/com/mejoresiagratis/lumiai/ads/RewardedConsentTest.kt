package com.mejoresiagratis.lumiai.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mejoresiagratis.lumiai.domain.entitlement.RecordRewardUseCase
import com.mejoresiagratis.lumiai.domain.entitlement.RewardProgress
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class RewardedConsentTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val state = MutableStateFlow(AdsConsentState())
    private val consent = mockk<AdsConsentManager> {
        every { this@mockk.state } returns this@RewardedConsentTest.state
        every { canRequestAds } answers { state.value.allowed && !state.value.updating }
    }
    private val record = mockk<RecordRewardUseCase>()
    private var uid = "A"
    private val auth = mockk<AuthRepository> { every { currentUid() } answers { uid } }
    private val loads = mutableListOf<RewardedAdLoadCallback>()
    private lateinit var controller: RewardedAdController
    private val ad = mockk<RewardedAd>(relaxed = true)

    @Before fun setUp() {
        mockkStatic(MobileAds::class)
        mockkStatic(RewardedAd::class)
        every { MobileAds.initialize(any<Context>(), any<OnInitializationCompleteListener>()) } answers {
            secondArg<OnInitializationCompleteListener>().onInitializationComplete(mockk(relaxed = true))
        }
        every { RewardedAd.load(any<Context>(), any<String>(), any<AdRequest>(), capture(loads)) } just Runs
        controller = RewardedAdController(mockk(relaxed = true), record, consent, auth)
    }
    @After fun tearDown() { unmockkAll() }

    @Test fun `no initialization or load before consent`() = runTest {
        runCurrent()
        controller.initializeAndPreload()
        controller.preload()
        verify(exactly = 0) { MobileAds.initialize(any<Context>(), any<OnInitializationCompleteListener>()) }
        assertTrue(loads.isEmpty())
    }

    @Test fun `revoked consent discards late load and allows fresh request`() = runTest {
        state.value = AdsConsentState(revision = 1, allowed = true)
        runCurrent()
        val old = loads.single()
        state.value = AdsConsentState(revision = 2, updating = true)
        runCurrent()
        old.onAdLoaded(ad)
        assertFalse(controller.isReady.value)
        state.value = AdsConsentState(revision = 3, allowed = true)
        runCurrent()
        assertEquals(2, loads.size)
        loads.last().onAdLoaded(ad)
        assertTrue(controller.isReady.value)
    }

    @Test fun `double tap cannot reuse ad and late reward cannot cross accounts`() = runTest {
        state.value = AdsConsentState(revision = 1, allowed = true)
        runCurrent()
        loads.single().onAdLoaded(ad)
        val reward = slot<OnUserEarnedRewardListener>()
        every { ad.show(any<Activity>(), capture(reward)) } just Runs
        var unavailable = 0
        controller.showIfAvailable(mockk(), {}, { unavailable++ })
        controller.showIfAvailable(mockk(), {}, { unavailable++ })
        verify(exactly = 1) { ad.show(any<Activity>(), any()) }
        assertEquals(1, unavailable)
        uid = "B"
        reward.captured.onUserEarnedReward(mockk())
        runCurrent()
        coVerify(exactly = 0) { record.invoke(any(), any()) }
    }

    @Test fun `duplicate reward callback credits once`() = runTest {
        state.value = AdsConsentState(revision = 1, allowed = true)
        runCurrent()
        loads.single().onAdLoaded(ad)
        val reward = slot<OnUserEarnedRewardListener>()
        every { ad.show(any<Activity>(), capture(reward)) } just Runs
        coEvery { record.invoke(any(), "A") } returns RewardProgress.Outcome(1, false)
        controller.showIfAvailable(mockk(), {}, {})
        reward.captured.onUserEarnedReward(mockk())
        reward.captured.onUserEarnedReward(mockk())
        runCurrent()
        coVerify(exactly = 1) { record.invoke(any(), "A") }
    }
}
