package com.mejoresiagratis.lumiai.domain.music

import kotlin.math.abs
import kotlin.math.min

/**
 * Sensibilidad del detector de ritmo. Factor multiplicador del umbral adaptativo:
 * mas bajo = salta con menos contraste (mas destellos, mas falsos positivos).
 */
enum class MusicSensitivity(val factor: Float) {
    BAJA(1.55f),
    MEDIA(1.38f),
    ALTA(1.22f)
}

/** Un golpe de ritmo detectado. [strength] 0..1 segun cuanto sobresale del entorno. */
data class Beat(val strength: Float, val bpmEstimate: Int?)

/**
 * Detector de ritmo por energia (DSP puro, sin IA y sin red — honestidad de marca):
 *
 * 1. Filtro paso-bajo de un polo (~200 Hz) para fijarse en bombo/graves y no en
 *    voces o platillos: es lo que hace que el destello se sienta "musical".
 * 2. Energia RMS por ventana (hop de [hopSize] muestras) sobre una historia de ~1 s.
 * 3. Umbral ADAPTATIVO: media local x factor de sensibilidad, endurecido cuando la
 *    varianza relativa es alta (musica muy comprimida no dispara en cada ventana).
 * 4. Refractario CONSCIENTE DEL TEMPO: estima el BPM con la mediana de los ultimos
 *    intervalos entre golpes y no permite un nuevo golpe antes del ~45% del periodo
 *    (evita el doble-disparo del "bam" simple y sigue aceleraciones reales).
 * 5. [Beat.strength] proporcional a cuanto sobresale la energia: el destello puede
 *    ser mas brillante y mas largo en los golpes fuertes.
 *
 * Modelo puro y deterministico: [onEnergy] es `internal` para poder probar la FSM
 * con secuencias sinteticas sin audio real.
 */
class BeatDetector(
    private val sampleRate: Int = 44_100,
    val hopSize: Int = 1_024,
    @Volatile var sensitivity: MusicSensitivity = MusicSensitivity.MEDIA
) {
    private companion object {
        const val HISTORY = 43              // ~1 s de historia con hop 1024 @ 44.1 kHz
        const val SILENCE_FLOOR = 1e-5f     // energia media minima para considerar "hay musica"
        const val MIN_REFRACTORY_MS = 180L  // techo de ~333 BPM percibidos
        const val MAX_REFRACTORY_MS = 600L
        const val DEFAULT_REFRACTORY_MS = 250L
        const val INTERVALS_TRACKED = 8
        const val LOWPASS_CUTOFF_HZ = 200f
        const val BPM_MIN = 60
        const val BPM_MAX = 200
    }

    // Filtro paso-bajo de un polo: alpha = dt / (RC + dt)
    private val lpAlpha: Float = run {
        val dt = 1f / sampleRate
        val rc = 1f / (2f * Math.PI.toFloat() * LOWPASS_CUTOFF_HZ)
        dt / (rc + dt)
    }
    private var lpState = 0f

    private val history = FloatArray(HISTORY)
    private var historyCount = 0
    private var historyIndex = 0

    private var lastBeatAtMs = 0L
    private val intervals = ArrayDeque<Long>(INTERVALS_TRACKED)

    /**
     * Alimenta [count] muestras PCM 16-bit mono. Devuelve un [Beat] si en este bloque
     * se detecto un golpe (como mucho uno por hop). [nowMs] inyectable para tests.
     */
    fun feed(samples: ShortArray, count: Int, nowMs: Long): Beat? {
        var i = 0
        var result: Beat? = null
        while (i + hopSize <= count) {
            var sum = 0f
            var j = i
            val end = i + hopSize
            while (j < end) {
                // normaliza a -1..1 y filtra graves
                val x = samples[j] / 32768f
                lpState += lpAlpha * (x - lpState)
                sum += lpState * lpState
                j++
            }
            val energy = sum / hopSize
            val beat = onEnergy(energy, nowMs)
            if (beat != null) result = beat
            i += hopSize
        }
        return result
    }

    /** Nucleo de decision sobre una energia ya calculada. Expuesto internal para tests. */
    internal fun onEnergy(energy: Float, nowMs: Long): Beat? {
        // 1. actualizar historia
        val filled = historyCount >= HISTORY
        val avg: Float
        val relVariance: Float
        if (filled) {
            var s = 0f
            for (e in history) s += e
            avg = s / HISTORY
            var v = 0f
            for (e in history) {
                val d = e - avg
                v += d * d
            }
            val variance = v / HISTORY
            relVariance = if (avg > 0f) min(1f, variance / (avg * avg)) else 0f
        } else {
            avg = 0f
            relVariance = 0f
        }
        history[historyIndex] = energy
        historyIndex = (historyIndex + 1) % HISTORY
        if (historyCount < HISTORY) historyCount++

        if (!filled) return null
        // 2. puerta de silencio: sin musica no hay destellos
        if (avg < SILENCE_FLOOR) return null

        // 3. umbral adaptativo (endurecido con varianza relativa alta)
        val threshold = avg * sensitivity.factor * (1f + 0.25f * relVariance)
        if (energy <= threshold) return null

        // 4. refractario consciente del tempo
        val refractory = tempoRefractoryMs()
        if (lastBeatAtMs != 0L && nowMs - lastBeatAtMs < refractory) return null

        if (lastBeatAtMs != 0L) {
            val interval = nowMs - lastBeatAtMs
            // solo intervalos plausibles alimentan la estimacion de tempo
            if (interval in (60_000L / BPM_MAX)..(60_000L / BPM_MIN)) {
                if (intervals.size == INTERVALS_TRACKED) intervals.removeFirst()
                intervals.addLast(interval)
            }
        }
        lastBeatAtMs = nowMs

        // 5. fuerza del golpe: cuanto sobresale, mapeado a 0.3..1
        val over = (energy / threshold) - 1f
        val strength = min(1f, 0.3f + over / 1.5f)
        return Beat(strength = strength, bpmEstimate = bpmEstimate())
    }

    /** Mediana de los intervalos recientes -> BPM, o null hasta tener 3 golpes utiles. */
    fun bpmEstimate(): Int? {
        if (intervals.size < 3) return null
        val sorted = intervals.sorted()
        val median = sorted[sorted.size / 2]
        return (60_000L / median).toInt().coerceIn(BPM_MIN, BPM_MAX)
    }

    private fun tempoRefractoryMs(): Long {
        if (intervals.size < 3) return DEFAULT_REFRACTORY_MS
        val sorted = intervals.sorted()
        val median = sorted[sorted.size / 2]
        return (median * 0.45f).toLong().coerceIn(MIN_REFRACTORY_MS, MAX_REFRACTORY_MS)
    }

    /** Reinicia estado (al parar/arrancar la escucha). */
    fun reset() {
        lpState = 0f
        historyCount = 0
        historyIndex = 0
        lastBeatAtMs = 0L
        intervals.clear()
    }
}

/** Traduce la fuerza de un golpe a destello: brillo 35..100 % y duracion 60..140 ms. */
object BeatFlashMapper {
    fun intensityPercent(strength: Float): Int =
        (35 + (strength.coerceIn(0f, 1f) * 65)).toInt()

    fun durationMs(strength: Float): Long =
        (60 + (strength.coerceIn(0f, 1f) * 80)).toLong()
}
