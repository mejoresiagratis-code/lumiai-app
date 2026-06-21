package com.mejoresiagratis.lumiai.ui.home

import app.cash.turbine.test
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.util.FakeFlashStateRepository
import com.mejoresiagratis.lumiai.util.FakeTorchController
import com.mejoresiagratis.lumiai.util.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = FakeFlashStateRepository()
    private val torch: TorchController = FakeTorchController(hasFlash = true, maxIntensityLevel = 100)
    private val engine: EngineController = mockk(relaxed = true)

    private fun vm() = FlashViewModel(repo, engine, torch)

    @Test
    fun `toggle on starts engine and reflects state`() = runTest {
        val vm = vm()
        vm.uiState.test {
            assertEquals(false, awaitItem().isOn)
            vm.toggle()
            assertTrue(awaitItem().isOn)
        }
        verify { engine.start() }
    }

    @Test
    fun `selectMode updates the mode in state`() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.selectMode(FlashMode.STROBE)
            assertEquals(FlashMode.STROBE, awaitItem().mode)
        }
    }

    @Test
    fun `updateSettings clamps out-of-range intensity`() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.updateSettings { it.copy(intensityLevel = 9999) }
            assertEquals(100, awaitItem().settings.intensityLevel)
        }
    }
}
