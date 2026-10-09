package com.mejoresiagratis.lumiai.data.sound

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.core.BaseOptions
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundDetectionEngine

/**
 * Envuelve el MediaPipe Audio Classifier (modelo YAMNet) y el AudioRecord para clasificar el
 * microfono en streaming. Estructura calcada del sample oficial de Google AI Edge.
 *
 * Cada ventana de clasificacion se reduce a un mapa etiqueta -> score y se pasa a
 * [SoundDetectionEngine], que decide (umbral/debounce/cooldown) que categorias alertar; las
 * emitidas se entregan por [onDetected].
 *
 * IMPORTANTE: requiere el modelo en `app/src/main/assets/yamnet.tflite`. Sin el modelo, la
 * inicializacion falla (se notifica por [onError]) y no se escucha nada. No se finge deteccion.
 */
class MediaPipeSoundClassifier(
    private val context: Context,
    private val engine: SoundDetectionEngine,
    private val allowedLabels: List<String>,
    private val onDetected: (SoundCategory) -> Unit,
    private val onError: (String) -> Unit = {},
    /** Scores de cada ventana clasificada — para la observabilidad en pantalla (QA 14-ago). */
    private val onWindow: (Map<String, Float>) -> Unit = {}
) {
    private var recorder: AudioRecord? = null
    private var captureLoop: ClassificationLoop? = null
    @Volatile private var stopped = false
    private var classifier: AudioClassifier? = null

    @SuppressLint("MissingPermission")
    fun start() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(YAMNET_MODEL)
                .build()
            val optionsBuilder = AudioClassifier.AudioClassifierOptions.builder()
                .setScoreThreshold(MIN_SCORE)
                .setMaxResults(MAX_RESULTS)
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.AUDIO_STREAM)
                .setResultListener(this::onStreamResult)
                .setErrorListener(this::onStreamError)
            if (allowedLabels.isNotEmpty()) {
                optionsBuilder.setCategoryAllowlist(allowedLabels)
            }
            val classifier = AudioClassifier.createFromOptions(context, optionsBuilder.build())
            this.classifier = classifier

            val recorder = classifier.createAudioRecord(
                AudioFormat.CHANNEL_IN_DEFAULT,
                SAMPLING_RATE_IN_HZ,
                BUFFER_SIZE_IN_BYTES
            )
            this.recorder = recorder
            recorder.startRecording()

            val lengthMs = (REQUIRE_INPUT_BUFFER_SIZE / SAMPLING_RATE_IN_HZ.toFloat()) * 1000f
            val interval = (lengthMs * (1 - DEFAULT_OVERLAP * 0.25)).toLong().coerceAtLeast(1L)
            captureLoop = ClassificationLoop(this::onStreamError).also {
                it.start(interval, this::classifyOnce)
            }
        } catch (e: IllegalStateException) {
            onError(e.message ?: "init failed")
            Log.e(TAG, "MediaPipe init error", e)
        } catch (e: RuntimeException) {
            onError(e.message ?: "init failed")
            Log.e(TAG, "MediaPipe init error", e)
        }
    }

    private fun classifyOnce() {
        val rec = recorder ?: return
        val audioData = AudioData.create(
            AudioDataFormat.create(rec.format),
            SAMPLING_RATE_IN_HZ
        )
        audioData.load(rec)
        if (!stopped) classifier?.classifyAsync(audioData, SystemClock.uptimeMillis())
    }

    private fun onStreamResult(result: AudioClassifierResult) {
        val categories = result.classificationResults().firstOrNull()
            ?.classifications()?.firstOrNull()
            ?.categories().orEmpty()
        if (stopped) return
        val scores = categories.associate { it.categoryName() to it.score() }
        onWindow(scores)
        val fired = engine.onWindow(scores, SystemClock.uptimeMillis())
        fired.forEach(onDetected)
    }

    private fun onStreamError(e: RuntimeException) {
        if (stopped) return
        onError(e.message ?: "stream error")
        Log.e(TAG, "MediaPipe stream error", e)
    }

    suspend fun stop() = withContext(NonCancellable + Dispatchers.IO) {
        stopped = true
        captureLoop?.stop()
        // Unblock the reader before joining it. Closing MediaPipe while classifyAsync is
        // still in flight races native teardown; releasing AudioRecord early races load().
        runCatching { recorder?.stop() }
        captureLoop?.awaitStopped()
        captureLoop = null
        try {
            classifier?.close()
        } finally {
            classifier = null
            recorder?.release()
            recorder = null
            engine.reset()
        }
    }

    companion object {
        private const val TAG = "SoundClassifier"
        const val YAMNET_MODEL = "yamnet.tflite"
        private const val SAMPLING_RATE_IN_HZ = 16000
        private const val EXPECTED_INPUT_LENGTH = 0.975f
        private const val DEFAULT_OVERLAP = 2
        private const val MAX_RESULTS = 5
        // Piso de ENTRADA, no de alerta: el umbral real lo aplica SoundDetectionEngine
        // por categoria. Bajado 0.3 -> 0.15 (QA 14-ago): un golpe de ~0.2 s dentro de la
        // ventana de ~1 s llega con el score DILUIDO y el piso 0.3 lo descartaba antes
        // de que el motor (ni la pantalla de "Oyendo") pudiera verlo siquiera.
        private const val MIN_SCORE = 0.15f
        private const val REQUIRE_INPUT_BUFFER_SIZE = SAMPLING_RATE_IN_HZ * EXPECTED_INPUT_LENGTH
        private const val BUFFER_SIZE_FACTOR = 2
        private val BUFFER_SIZE_IN_BYTES =
            (REQUIRE_INPUT_BUFFER_SIZE * Float.SIZE_BYTES * BUFFER_SIZE_FACTOR).toInt()
    }
}
