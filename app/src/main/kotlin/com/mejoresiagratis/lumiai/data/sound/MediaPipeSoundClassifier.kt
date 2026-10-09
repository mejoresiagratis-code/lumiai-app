package com.mejoresiagratis.lumiai.data.sound

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.core.BaseOptions
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundDetectionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Captures MIC PCM16 and classifies complete 975 ms YAMNet clips with 50% overlap.
 * A single worker owns capture, inference and the detection engine. AUDIO_CLIPS is intentional:
 * overlapping full windows must not be fed as consecutive, partially zero-padded stream blocks.
 */
class MediaPipeSoundClassifier(
    private val context: Context,
    private val engine: SoundDetectionEngine,
    private val onDetected: (SoundCategory) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onWindow: (Map<String, Float>, Float, Long) -> Unit = { _, _, _ -> }
) {
    private var recorder: AudioRecord? = null
    private var captureLoop: ClassificationLoop? = null
    private var classifier: AudioClassifier? = null
    @Volatile private var stopped = false
    private val windows = PcmAudioWindows()
    private val readBuffer = ShortArray(4_096)
    private var resultCount = 0L

    @SuppressLint("MissingPermission") // Service checks permission; construction/start failures propagate.
    fun start() {
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(YAMNET_MODEL).build())
            .setRunningMode(RunningMode.AUDIO_CLIPS)
            // Return all scores. Category thresholds belong to SoundDetectionEngine; the UI
            // must also show speech/music/silence so a working classifier never looks stalled.
            .setScoreThreshold(0f)
            .build()
        classifier = AudioClassifier.createFromOptions(context, options)
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minimum > 0) { "Unsupported microphone format" }
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, SAMPLE_RATE * Short.SIZE_BYTES * 2)
        )
        recorder = record
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed" }
        record.startRecording()
        check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start" }
        captureLoop = ClassificationLoop { error ->
            if (!stopped) onError(error.message ?: "Audio classification failed")
        }.also { it.start(50, this::captureOnce) }
    }

    private fun captureOnce() {
        val record = recorder ?: return
        val read = record.read(readBuffer, 0, readBuffer.size, AudioRecord.READ_NON_BLOCKING)
        check(read >= 0) { "Microphone read failed: $read" }
        if (stopped || read == 0) return
        windows.append(readBuffer, read) { samples ->
            if (!stopped) classifyWindow(samples)
        }
    }

    private fun classifyWindow(samples: ShortArray) {
        val format = AudioDataFormat.builder().setNumOfChannels(1).setSampleRate(SAMPLE_RATE.toFloat()).build()
        val audio = AudioData.create(format, samples.size)
        audio.load(samples)
        val result = checkNotNull(classifier).classify(audio)
        if (stopped) return
        val scores = mutableMapOf<String, Float>()
        result.classificationResults().forEach { window ->
            window.classifications().forEach { head ->
                head.categories().forEach { category ->
                    val label = category.categoryName()
                    scores[label] = maxOf(scores[label] ?: 0f, category.score())
                }
            }
        }
        val rms = sqrt(samples.sumOf { val value = it / 32768.0; value * value } / samples.size)
        val levelDb = (20 * log10(rms.coerceAtLeast(0.000001))).toFloat()
        onWindow(scores, levelDb, ++resultCount)
        engine.onWindow(scores, SystemClock.elapsedRealtime()).forEach(onDetected)
    }

    suspend fun stop() = withContext(NonCancellable + Dispatchers.IO) {
        stopped = true
        captureLoop?.stop()
        runCatching { recorder?.stop() }
        // Do not close native inference or release the microphone while the worker uses them.
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
        const val YAMNET_MODEL = "yamnet.tflite"
        private const val SAMPLE_RATE = 16_000
    }
}
