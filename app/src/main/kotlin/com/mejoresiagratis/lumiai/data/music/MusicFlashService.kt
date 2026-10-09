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
import com.mejoresiagratis.lumiai.data.system.NotificationIds
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.entitlement.whileAiAccess
import com.mejoresiagratis.lumiai.domain.entitlement.ProAccessMonitor
import com.mejoresiagratis.lumiai.data.session.HardwareSessionCoordinator
import com.mejoresiagratis.lumiai.domain.music.BeatDetector
import com.mejoresiagratis.lumiai.domain.music.BeatFlashMapper
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.domain.repository.MusicConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
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
 * El coordinador espera la liberacion de la sesion anterior antes de abrir el microfono.
 * Requiere RECORD_AUDIO; sin permiso o sin flash, el servicio se detiene solo.
 */
@AndroidEntryPoint
class MusicFlashService : Service() {

    @Inject lateinit var torch: TorchController
    @Inject lateinit var configRepo: MusicConfigRepository
    @Inject lateinit var flashState: FlashStateRepository
    @Inject lateinit var sessions: HardwareSessionCoordinator
    @Inject lateinit var proAccess: ProAccessMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ready = false

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        // El permiso se comprueba ANTES de startForeground: un FGS de tipo microfono
        // sin RECORD_AUDIO lanza SecurityException en API 34+ (crash, no fallo suave).
        val micGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted || !torch.hasFlash) {
            // Sin permiso/LED el modo no puede correr: el orbe vuelve a apagado
            // para que la UI no quede encendida sin sesion detras.
            flashState.setOn(false)
            stopSelf()
            return
        }
        try {
            startInForeground()
            ready = true
        } catch (_: RuntimeException) {
            flashState.setOn(false)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ready) return START_NOT_STICKY
        scope.launch {
            try {
                sessions.runSession(onFinished = { flashState.setOn(false) }) { leasedTorch ->
                    proAccess.hasAiAccess.whileAiAccess(
                        onDenied = { updateNotification(R.string.music_notif_no_pro) }
                    ) {
                        flashState.setOn(true)
                        coroutineScope {
                            val detector = BeatDetector()
                            launch { configRepo.sensitivity.collect { detector.sensitivity = it } }
                            launch { leasedTorch.externalOffEvents.collect { stopSelf(startId) } }
                            // Capture and every pulse belong to this scope, including cleanup.
                            listen(detector, leasedTorch)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: RuntimeException) {
                updateNotification(R.string.music_notif_error)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun listen(detector: BeatDetector, torch: TorchController) = coroutineScope {
        val sampleRate = 44_100
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBuffer > 0) { "Unsupported microphone buffer" }
        // Permission may change while this request waits for the previous session.
        if (ContextCompat.checkSelfPermission(this@MusicFlashService, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission revoked")
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, detector.hopSize * 4)
        )
        var flashJob: Job? = null
        try {
            check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed" }
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone unavailable" }
            val buffer = ShortArray(detector.hopSize)
            var buffered = 0
            var consecutiveErrors = 0
            while (isActive) {
                // Nonblocking reads make cancellation independent of microphone input.
                // Preserve partial reads: BeatDetector requires a complete hop.
                val read = record.read(buffer, buffered, buffer.size - buffered, AudioRecord.READ_NON_BLOCKING)
                when {
                    read > 0 -> {
                        consecutiveErrors = 0
                        buffered += read
                        if (buffered == buffer.size) {
                            val beat = detector.feed(buffer, buffered, android.os.SystemClock.elapsedRealtime())
                            buffered = 0
                            if (beat != null) {
                                // Join the previous finally before turning the LED on again.
                                flashJob?.cancelAndJoin()
                                flashJob = launch {
                                    try {
                                        torch.turnOn(BeatFlashMapper.intensityPercent(beat.strength))
                                        delay(BeatFlashMapper.durationMs(beat.strength))
                                    } finally { torch.pulseOff() }
                                }
                            }
                        }
                    }
                    read < 0 -> {
                        check(++consecutiveErrors < MAX_READ_ERRORS) { "Microphone read failed: $read" }
                        delay(READ_ERROR_BACKOFF_MS)
                    }
                    else -> delay(READ_EMPTY_BACKOFF_MS)
                }
            }
        } finally {
            flashJob?.cancel()
            runCatching { record.stop() }
            record.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notif = buildNotification(R.string.music_notif_listening)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(textRes: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(textRes))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    /** Actualiza el texto de la notificación FGS sin recrear el servicio. */
    private fun updateNotification(textRes: Int) {
        runCatching {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.notify(NOTIF_ID, buildNotification(textRes))
        }
    }

    companion object {
        private const val CHANNEL_ID = "music_flash"
        private const val NOTIF_ID = NotificationIds.MUSIC_FOREGROUND
        // Watchdog de lectura de audio.
        private const val MAX_READ_ERRORS = 20       // ~ errores seguidos antes de rendirse
        private const val READ_ERROR_BACKOFF_MS = 50L
        private const val READ_EMPTY_BACKOFF_MS = 10L

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
