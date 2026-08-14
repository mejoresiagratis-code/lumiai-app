package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.util.FakeTorchController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashEngineTest {

    @Test
    fun `continuous turns torch on immediately`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, MutableStateFlow(FlashSettings())) }
        runCurrent()
        assertTrue(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `changing intensity while on does not cycle the torch off`() = runTest {
        val torch = FakeTorchController(maxIntensityLevel = 100)
        val engine = FlashEngine(torch)
        val settings = MutableStateFlow(FlashSettings(intensityLevel = 50))
        val job = launch { engine.play(FlashMode.CONTINUOUS, settings) }
        runCurrent()
        assertTrue(torch.isOn)
        assertEquals(50, torch.lastIntensity)

        settings.value = FlashSettings(intensityLevel = 80)
        runCurrent()
        assertEquals(80, torch.lastIntensity)
        // Solo hubo un encendido: nunca se apago entre pasos del slider.
        assertEquals(listOf(true), torch.transitions)

        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }

    @Test
    fun `cancelling play always leaves the torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, MutableStateFlow(FlashSettings())) }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }

    @Test
    fun `strobe alterna apagado REAL entre pulsos (revert QA 14-ago)`() = runTest {
        // Revertido el experimento de "atenuar en vez de apagar" (v0.9.17): el hueco
        // del patron vuelve a ser un apagado real — el resplandor residual difuminaba
        // el contraste on/off. pulseOff() sigue siendo el gancho semantico del hueco.
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.STROBE, MutableStateFlow(FlashSettings(strobeHz = 10f))) }
        runCurrent(); assertTrue(torch.isOn); assertEquals(100, torch.lastIntensity)
        advanceTimeBy(50); runCurrent()
        assertFalse(torch.isOn) // apagado DE VERDAD entre pulsos
        assertEquals(1, torch.pulseOffCalls) // y llego via el gancho del hueco
        advanceTimeBy(50); runCurrent()
        assertTrue(torch.isOn)
        assertEquals(100, torch.lastIntensity)
        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }

    @Test
    fun `screen mode keeps the hardware torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SCREEN, MutableStateFlow(FlashSettings())) }
        runCurrent()
        assertFalse(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `sos alterna encendido y apagado REAL por simbolo (revert QA 14-ago)`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SOS_MORSE, MutableStateFlow(FlashSettings(morseUnitMs = 100L))) }
        runCurrent(); assertTrue(torch.isOn)
        advanceTimeBy(100); runCurrent()
        assertFalse(torch.isOn) // hueco entre simbolos: apagado DE VERDAD
        advanceTimeBy(100); runCurrent(); assertTrue(torch.isOn)
        job.cancelAndJoin()
        assertFalse(torch.isOn) // fin de sesion: apagado real (finally { turnOff() })
    }
}
