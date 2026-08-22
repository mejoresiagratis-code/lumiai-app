package com.mejoresiagratis.lumiai.data.music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.system.NotificationIds
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.entitlement.ProAccessMonitor
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
    @Inject lateinit var proAccess: ProAccessMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null
    private var flashJob: Job? = null
    private var recorder: AudioRecord? = null
    private val detector = BeatDetector()

    // Foco de audio: si otra app (llamada, grabadora, asistente) toma el audio, pausamos
    // los destellos sin matar el servicio y reanudamos al recuperarlo.
    @Volatile private var audioFocusLost = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                audioFocusLost = true
                runCatching { torch.turnOff() }
                updateNotification(R.string.music_notif_paused)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusLost = false
                updateNotification(R.string.music_notif_listening)
            }
        }
    }

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
        startInForeground()

        // El motor principal suelta el LED antes del show, pero SIN tocar isOn:
        // en Musica el orbe encendido representa ESTA sesion (una sola notificacion,
        // la de este servicio — QA 13-ago).
        engine.stop()

        // La sensibilidad se aplica en vivo sin reiniciar la escucha.
        scope.launch {
            configRepo.sensitivity.collect { detector.sensitivity = it }
        }
        // Apagado EXTERNO (boton "Desactivar" del sistema): sin esto, el siguiente
        // golpe detectado reencendia la luz sin saber que algo externo la habia
        // apagado (QA 13-ago). stopSelf() dispara onDestroy(), que ya deja isOn/torch
        // en su estado correcto.
        scope.launch { torch.externalOffEvents.collect { stopSelf() } }
        // REVOCACION EN EL SERVICIO (22-ago): ver stopWhenAccessLost().
        scope.launch { stopWhenAccessLost() }
        requestAudioFocus()
        startListening()
    }

    /**
     * Para el servicio cuando el acceso Pro se PIERDE (caducidad, logout, borrado de cuenta).
     * Solo actua en la TRANSICION de "tenia acceso" a "ya no": la primera emision puede llegar
     * en `false` mientras los permisos se cargan, y reaccionar a eso mataria el modo nada mas
     * empezar a sonar.
     */
    private suspend fun stopWhenAccessLost() {
        var hadAccess = false
        proAccess.hasAiAccess.collect { has ->
            if (has) {
                hadAccess = true
            } else if (hadAccess) {
                updateNotification(R.string.music_notif_no_pro)
                stopSelf()
            }
        }
    }

    private fun requestAudioFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am
        // No bloqueamos si no se concede: solo escuchamos el ambiente, no reproducimos.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = request
            runCatching { am.requestAudioFocus(request) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                am.requestAudioFocus(
                    focusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }
    }

    private fun startListening() {
        audioJob = scope.launch {
            val sampleRate = 44_100
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) { stopSelf(); return@launch }
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
                notifyError(); stopSelf(); return@launch
            } catch (_: IllegalArgumentException) {
                notifyError(); stopSelf(); return@launch
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release(); notifyError(); stopSelf(); return@launch
            }
            recorder = record
            detector.reset()
            // El micro puede estar ocupado por otra app: startRecording() puede tirar.
            try {
                record.startRecording()
            } catch (_: IllegalStateException) {
                record.release(); recorder = null; notifyError(); stopSelf(); return@launch
            }
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                record.release(); recorder = null; notifyError(); stopSelf(); return@launch
            }

            val buffer = ShortArray(detector.hopSize)
            // Watchdog: si read() falla o no entrega datos de forma persistente, no giramos
            // en vacío para siempre — paramos limpiamente tras un umbral de fallos seguidos.
            var consecutiveErrors = 0
            try {
                while (isActive) {
                    // Con el foco de audio perdido (llamada, otra app graba) no destellamos:
                    // seguimos leyendo para vaciar el buffer, pero sin disparar el flash.
                    val read = record.read(buffer, 0, buffer.size)
                    when {
                        read > 0 -> {
                            consecutiveErrors = 0
                            if (!audioFocusLost) {
                                val beat = detector.feed(buffer, read, System.currentTimeMillis())
                                if (beat != null) pulse(beat.strength)
                            }
                        }
                        // Códigos negativos de error de AudioRecord (DEAD_OBJECT, INVALID_OPERATION…)
                        read < 0 -> {
                            consecutiveErrors++
                            if (consecutiveErrors >= MAX_READ_ERRORS) {
                                notifyError(); stopSelf(); break
                            }
                            delay(READ_ERROR_BACKOFF_MS)
                        }
                        // read == 0: sin datos este ciclo; cedemos CPU y seguimos.
                        else -> delay(READ_EMPTY_BACKOFF_MS)
                    }
                }
            } finally {
                runCatching { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() }
                runCatching { record.release() }
                recorder = null
            }
        }
    }

    /**
     * Un destello por golpe: brillo y duracion proporcionales a su fuerza. Entre golpes,
     * pulseOff() hace un apagado REAL — el experimento de dejar un resplandor tenue se
     * revirtio el 14-ago tras el QA: difuminaba el contraste entre golpe y silencio. El
     * precio asumido es que el indicador del sistema de Samsung vuelve a parpadear.
     */
    private fun pulse(strength: Float) {
        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                torch.turnOn(BeatFlashMapper.intensityPercent(strength))
                delay(BeatFlashMapper.durationMs(strength))
            } finally {
                torch.pulseOff()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { flashState.setOn(false) }
        audioJob?.cancel()
        flashJob?.cancel()
        abandonAudioFocus()
        runCatching { torch.turnOff() }
        scope.cancel()
        super.onDestroy()
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { am.abandonAudioFocusRequest(it) } }
        } else {
            @Suppress("DEPRECATION")
            runCatching { am.abandonAudioFocus(focusListener) }
        }
        focusRequest = null
        audioManager = null
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

    /** Señala un fallo de micrófono en la notificación antes de parar. */
    private fun notifyError() {
        runCatching { torch.turnOff() }
        updateNotification(R.string.music_notif_error)
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
