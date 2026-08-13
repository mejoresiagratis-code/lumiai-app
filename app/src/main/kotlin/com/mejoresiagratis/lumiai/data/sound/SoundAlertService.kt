package com.mejoresiagratis.lumiai.data.sound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertConfigRepository
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertStateRepository
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertFlash
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundDetectionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano (tipo microfono) que escucha y clasifica sonidos en el dispositivo y,
 * al reconocer una categoria activa, avisa segun su canal configurado: destello del LED (patron
 * por ritmo), parpadeo de pantalla (ScreenFlashActivity via full-screen-intent) o ambos. Si se
 * pidio flash pero el dispositivo no tiene, cae a pantalla para no dejar sin aviso.
 *
 * Lee la configuracion persistida (DataStore) al arrancar; los cambios de categorias/sensibilidad
 * /canal se aplican al reiniciar la escucha (el clasificador no se reconstruye en vivo). Requiere
 * RECORD_AUDIO y el modelo yamnet.tflite en assets: sin ellos sigue vivo pero no avisa.
 */
@AndroidEntryPoint
class SoundAlertService : Service() {

    @Inject lateinit var torch: TorchController
    @Inject lateinit var configRepo: SoundAlertConfigRepository
    @Inject lateinit var listeningState: SoundAlertStateRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var classifier: MediaPipeSoundClassifier? = null
    @Volatile private var currentConfig: SoundAlertConfig = SoundAlertConfig()
    private var flashJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        // CRASH-LOOP arreglado (QA 13-ago): startForeground de tipo microfono SIN el
        // permiso RECORD_AUDIO lanza SecurityException en API 34+. Si ademas el sistema
        // reintentaba (STICKY), la app moria en cada arranque hasta limpiar datos.
        // Orden correcto: permiso primero; sin el, parada limpia antes del foreground.
        val micGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            stopSelf()
            return
        }
        runCatching { startInForeground() }.onFailure {
            // Cinturon para OEMs con politicas FGS propias: parada suave, jamas crash.
            stopSelf()
            return
        }
        // Escucha REALMENTE arrancada (servicio vivo en primer plano): la pantalla deja
        // de fiarse de un flag local optimista y refleja esto (QA 13-ago).
        listeningState.setListening(true)
        // Apagado EXTERNO (boton "Desactivar" del sistema) durante un destello activo
        // (QA 13-ago). stopSelf() dispara la limpieza normal del servicio.
        scope.launch { torch.externalOffEvents.collect { stopSelf() } }
        scope.launch {
            // Cinturon de seguridad (QA 13-ago): SIN esto, cualquier fallo aqui dentro
            // (config corrupta, MediaPipe, lo que sea) tumbaba TODA LA APP — un
            // SupervisorJob sin manejador de excepciones no absorbe fallos de sus hijos,
            // solo evita que se cancelen entre si. Ahora degrada: se para el servicio,
            // la app sigue viva y la pantalla vuelve sola a "Iniciar".
            runCatching {
                val config = configRepo.config.first()
                currentConfig = config
                val engine = SoundDetectionEngine(config)
                val classifier = MediaPipeSoundClassifier(
                    context = applicationContext,
                    engine = engine,
                    allowedLabels = config.activeLabels().toList(),
                    onDetected = { category -> onDetected(category) },
                    onError = { /* sin modelo o sin permiso: sigue sin avisos */ }
                )
                this@SoundAlertService.classifier = classifier
                classifier.start()
            }.onFailure { stopSelf() }
        }
    }

    // NOT_STICKY: la escucha se (re)activa solo por accion del usuario. Un servicio de
    // microfono resucitado por el sistema sin contexto es el ingrediente del crash-loop.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        listeningState.setListening(false)
        flashJob?.cancel()
        classifier?.stop()
        classifier = null
        runCatching { torch.turnOff() }
        scope.cancel()
        super.onDestroy()
    }

    private fun onDetected(category: SoundCategory) {
        notifyDetection(category)
        val channel = currentConfig.channel(category)
        val flashed = channel.usesFlash && torch.hasFlash
        if (flashed) flash(category)
        // Pantalla si el usuario lo pidio, o como caida cuando se pidio flash pero no hay LED.
        if (channel.usesScreen || (channel.usesFlash && !flashed)) screenFlash(category)
    }

    private fun screenFlash(category: SoundCategory) {
        val intent = Intent(this, ScreenFlashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenFlashActivity.EXTRA_PATTERN, SoundAlertFlash.patternFor(category))
        }
        val pending = PendingIntent.getActivity(
            this,
            category.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sa_notif_detected))
            .setContentText(getString(category.labelRes()))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(SCREEN_NOTIF_ID, notif)
    }

    private fun flash(category: SoundCategory) {
        if (!torch.hasFlash) return
        flashJob?.cancel()
        flashJob = scope.launch {
            val pattern = SoundAlertFlash.patternFor(category)
            // Alerta = brillo maximo. turnOn espera un PORCENTAJE (1..100), igual que FlashEngine;
            // pasar maxIntensityLevel (nivel bruto del HW) daba el nivel minimo en algunos moviles.
            val level = FlashSettings.MAX_INTENSITY
            try {
                var i = 0
                while (i < pattern.size) {
                    torch.turnOn(level)
                    delay(pattern[i])
                    // Hueco DENTRO del patron (sigue "avisando"): pulseOff experimental
                    // (QA 13-ago) — evita el parpadeo del indicador del sistema en cada
                    // destello. El apagado real solo llega al terminar todo el patron.
                    torch.pulseOff()
                    if (i + 1 < pattern.size) delay(pattern[i + 1])
                    i += 2
                }
            } finally {
                torch.turnOff()
            }
        }
    }

    private fun startInForeground() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.sa_notif_listening))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun notifyDetection(category: SoundCategory) {
        val mgr = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sa_notif_detected))
            .setContentText(getString(category.labelRes()))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        mgr.notify(NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "sound_alert"
        private const val NOTIF_ID = 2
        private const val SCREEN_NOTIF_ID = 3

        fun start(context: Context) {
            ensureChannel(context)
            ContextCompat.startForegroundService(
                context, Intent(context, SoundAlertService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SoundAlertService::class.java))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(NotificationManager::class.java)
                if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                    mgr.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            context.getString(R.string.sa_title),
                            NotificationManager.IMPORTANCE_HIGH
                        )
                    )
                }
            }
        }
    }
}
