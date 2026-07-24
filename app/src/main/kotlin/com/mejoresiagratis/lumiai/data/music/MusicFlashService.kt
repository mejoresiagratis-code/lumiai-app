package com.mejoresiagratis.lumiai.data.music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.music.BeatDetector
import com.mejoresiagratis.lumiai.domain.music.BeatFlashMapper
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.domain.repository.MusicConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano (tipo microfono) del modo Musica: escucha el ambiente,
 * detecta los golpes de ritmo con [BeatDetector] (DSP puro, nada se graba ni se sube)
 * y dispara el flash con brillo y duracion proporcionales a la fuerza de cada golpe.
 *
 * Respeta la regla de oro (una sola clase toca el LED): todo pasa por [TorchController].
 * Al arrancar apaga el motor principal si estaba encendido para no pelear por el LED.
 * Requiere RECORD_AUDIO; sin permiso o sin flash, el servicio se detiene solo.
 */
@AndroidEntryPoint
class MusicFlashService : Service() {

    @Inject lateinit var torch: TorchController
    @Inject lateinit var configRepo: MusicConfigRepository
    @Inject lateinit var flashState: FlashStateRepository
    @Inject lateinit var engine: EngineController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null
    private var flashJob: Job? = null
    private var recorder: AudioRecord? = null
    private val detector = BeatDetector()

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startInForeground()

        val micGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted || !torch.hasFlash) {
            stopSelf()
            return
        }

        // El motor principal suelta el LED antes de empezar el show.
        if (flashState.isOn.value) {
            flashState.setOn(false)
            engine.stop()
        }

        // La sensibilidad se aplica en vivo sin reiniciar la escucha.
        scope.launch {
            configRepo.sensitivity.collect { detector.sensitivity = it }
        }
        startListening()
    }

    private fun startListening() {
        audioJob = scope.launch {
            val sampleRate = 44_100
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuffer, detector.hopSize * 4)
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (_: SecurityException) {
                stopSelf(); return@launch
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release(); stopSelf(); return@launch
            }
            recorder = record
            detector.reset()
            record.startRecording()
            val buffer = ShortArray(detector.hopSize)
            try {
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val beat = detector.feed(buffer, read, System.currentTimeMillis())
                        if (beat != null) pulse(beat.strength)
                    }
                }
            } finally {
                runCatching { record.stop() }
                record.release()
                recorder = null
            }
        }
    }

    /** Un destello por golpe: brillo y duracion proporcionales a su fuerza. */
    private fun pulse(strength: Float) {
        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                torch.turnOn(BeatFlashMapper.intensityPercent(strength))
                delay(BeatFlashMapper.durationMs(strength))
            } finally {
                torch.turnOff()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioJob?.cancel()
        flashJob?.cancel()
        runCatching { torch.turnOff() }
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.music_notif_listening))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    companion object {
        private const val CHANNEL_ID = "music_flash"
        private const val NOTIF_ID = 4

        fun start(context: Context) {
            ensureChannel(context)
            ContextCompat.startForegroundService(
                context, Intent(context, MusicFlashService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MusicFlashService::class.java))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(NotificationManager::class.java)
                if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                    mgr.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            context.getString(R.string.music_title),
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )
                }
            }
        }
    }
}
