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
    fun `strobe stays hardware-on and only dims between pulses (QA 13-ago)`() = runTest {
        // Con intensidad variable (por defecto en el fake), el hueco del patron usa
        // pulseOff(): la linterna NUNCA se apaga de verdad a mitad de Estrobo — solo
        // baja al minimo. Evita que el indicador del sistema parpadee en cada pulso.
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.STROBE, MutableStateFlow(FlashSettings(strobeHz = 10f))) }
        runCurrent(); assertTrue(torch.isOn); assertEquals(100, torch.lastIntensity)
        advanceTimeBy(50); runCurrent()
        assertTrue(torch.isOn) // sigue "encendida": sin apagado real intermedio
        assertEquals(1, torch.lastIntensity)
        assertEquals(1, torch.pulseOffCalls)
        advanceTimeBy(50); runCurrent()
        assertEquals(100, torch.lastIntensity)
        job.cancelAndJoin()
        assertFalse(torch.isOn) // al terminar, apagado REAL — turnOff(), no pulseOff()
    }

    @Test
    fun `strobe falls back to a real off on hardware without variable strength`() = runTest {
        // Dispositivo sin niveles de intensidad (maxIntensityLevel=1): pulseOff() cae a
        // un apagado real identico al comportamiento anterior — cero regresion ahi.
        val torch = FakeTorchController(maxIntensityLevel = 1)
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.STROBE, MutableStateFlow(FlashSettings(strobeHz = 10f))) }
        runCurrent(); assertTrue(torch.isOn)
        advanceTimeBy(50); runCurrent()
        assertFalse(torch.isOn) // apagado real, como antes de este cambio
        job.cancelAndJoin()
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
    fun `sos starts with a short flash and never truly turns off mid-pattern`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SOS_MORSE, MutableStateFlow(FlashSettings(morseUnitMs = 100L))) }
        runCurrent(); assertTrue(torch.isOn)
        advanceTimeBy(100); runCurrent()
        assertTrue(torch.isOn) // hueco entre simbolos: pulseOff(), no apagado real
        assertEquals(1, torch.lastIntensity)
        advanceTimeBy(100); runCurrent(); assertTrue(torch.isOn)
        job.cancelAndJoin()
        assertFalse(torch.isOn) // fin de sesion: SIEMPRE apagado real (finally { turnOff() })
    }
}
