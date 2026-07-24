package com.mejoresiagratis.lumiai.domain.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatDetectorTest {

    /** Rellena la historia (~1 s) con energia base para armar el umbral adaptativo. */
    private fun BeatDetector.warmup(base: Float = 0.001f, nowMs: Long = 0L): Long {
        var t = nowMs
        repeat(50) { onEnergy(base, t); t += 23 }
        return t
    }

    @Test
    fun `en silencio no hay golpes`() {
        val d = BeatDetector()
        var t = 0L
        repeat(120) {
            assertNull(d.onEnergy(0f, t))
            t += 23
        }
    }

    @Test
    fun `un pico claro sobre el fondo dispara un golpe con fuerza`() {
        val d = BeatDetector()
        val t = d.warmup()
        val beat = d.onEnergy(0.02f, t)
        assertTrue("esperaba golpe", beat != null)
        assertTrue("fuerza en 0.3..1", beat!!.strength in 0.3f..1f)
    }

    @Test
    fun `el refractario evita el doble disparo del mismo golpe`() {
        val d = BeatDetector()
        val t = d.warmup()
        assertTrue(d.onEnergy(0.02f, t) != null)
        // 50 ms despues: mismo golpe aun sonando -> NO debe re-disparar
        assertNull(d.onEnergy(0.02f, t + 50))
        // relleno de base y golpe nuevo pasado el refractario -> si
        var tt = t + 73
        repeat(13) { d.onEnergy(0.001f, tt); tt += 23 }
        assertTrue(d.onEnergy(0.02f, t + 400) != null)
    }

    @Test
    fun `un tren de golpes a 120 bpm estima el tempo`() {
        val d = BeatDetector()
        var t = d.warmup()
        // 120 BPM = golpe cada 500 ms; entre golpes, energia base cada 23 ms
        repeat(8) {
            d.onEnergy(0.02f, t)
            var tt = t + 23
            while (tt < t + 500) { d.onEnergy(0.001f, tt); tt += 23 }
            t += 500
        }
        val bpm = d.bpmEstimate()
        assertTrue("bpm estimado", bpm != null)
        assertTrue("bpm ~120 (fue $bpm)", bpm!! in 110..130)
    }

    @Test
    fun `golpe mas fuerte produce mas fuerza`() {
        val d1 = BeatDetector()
        val t1 = d1.warmup()
        val weak = d1.onEnergy(0.0021f, t1)!!.strength

        val d2 = BeatDetector()
        val t2 = d2.warmup()
        val strong = d2.onEnergy(0.05f, t2)!!.strength

        assertTrue("fuerte > debil ($strong vs $weak)", strong > weak)
        assertEquals(1f, strong, 0.0001f)
    }

    @Test
    fun `mapper de destello escala brillo y duracion`() {
        assertEquals(35, BeatFlashMapper.intensityPercent(0f))
        assertEquals(100, BeatFlashMapper.intensityPercent(1f))
        assertEquals(60L, BeatFlashMapper.durationMs(0f))
        assertEquals(140L, BeatFlashMapper.durationMs(1f))
    }
}
