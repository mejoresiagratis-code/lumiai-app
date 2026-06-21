package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.util.FakeTorchController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashEngineTest {

    @Test
    fun `continuous turns torch on immediately`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, FlashSettings()) }
        runCurrent()
        assertTrue(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `cancelling play always leaves the torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, FlashSettings()) }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }

    @Test
    fun `strobe toggles at the configured frequency`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.STROBE, FlashSettings(strobeHz = 10f)) }
        runCurrent(); assertTrue(torch.isOn)
        advanceTimeBy(50); runCurrent(); assertFalse(torch.isOn)
        advanceTimeBy(50); runCurrent(); assertTrue(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `screen mode keeps the hardware torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SCREEN, FlashSettings()) }
        runCurrent()
        assertFalse(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `sos starts with a short flash`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SOS_MORSE, FlashSettings(morseUnitMs = 100L)) }
        runCurrent(); assertTrue(torch.isOn)
        advanceTimeBy(100); runCurrent(); assertFalse(torch.isOn)
        advanceTimeBy(100); runCurrent(); assertTrue(torch.isOn)
        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }
}
