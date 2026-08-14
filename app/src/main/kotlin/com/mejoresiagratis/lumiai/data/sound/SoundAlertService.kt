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
import kotlinx.coroutines.flow.collectLatest
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
            listeningState.setStopReason("RECORD_AUDIO no concedido")
            stopSelf()
            return
        }
        runCatching { startInForeground() }.onFailure { e ->
            // Cinturon para OEMs con politicas FGS propias: parada suave, jamas crash.
            listeningState.setStopReason("startForeground: ${e.javaClass.simpleName}: ${e.message}")
            stopSelf()
            return
        }
        // Escucha REALMENTE arrancada: limpiar cualquier motivo de fallo anterior y
        // reflejarlo (QA 13-ago). Si algo la mata despues, el motivo quedara registrado
        // y la pantalla lo mostrara — diagnostico en el propio movil, no a ciegas.
        listeningState.setStopReason(null)
        listeningState.setListening(true)
        // Apagado EXTERNO de la linterna (boton "Desactivar" del sistema): corta SOLO el
        // destello en curso, NO la escucha entera — apagar la luz no es querer dejar de
        // escuchar (correccion de diseno, QA 14-ago: el stopSelf() anterior era un
        // candidato a matar el servicio entero por un evento que no lo justificaba).
        scope.launch { torch.externalOffEvents.collect { flashJob?.cancel() } }
        scope.launch {
            // Cinturon de seguridad (QA 13-ago): SIN esto, cualquier fallo aqui dentro
            // (config corrupta, MediaPipe, lo que sea) tumbaba TODA LA APP — un
            // SupervisorJob sin manejador de excepciones no absorbe fallos de sus hijos,
            // solo evita que se cancelen entre si. Ahora degrada CON DIAGNOSTICO: el
            // motivo exacto (clase + mensaje de la excepcion) queda visible en pantalla.
            runCatching {
                // CONFIG EN VIVO (QA 14-ago, captura de Pablo: con Telefono desactivado
                // seguia oyendo "Telephone" — la config se congelaba en un .first() al
                // arrancar y los interruptores tocados DURANTE la escucha no llegaban al
                // clasificador; activar Golpes en marcha no metia "Knock" en la allowlist).
                // Cada cambio de config reconstruye clasificador+motor con la foto nueva.
                configRepo.config.collectLatest { config ->
                    currentConfig = config
                    this@SoundAlertService.classifier?.stop()
                    val engine = SoundDetectionEngine(config)
                    val classifier = MediaPipeSoundClassifier(
                        context = applicationContext,
                        engine = engine,
                        allowedLabels = config.activeLabels().toList(),
                        onDetected = { category -> onDetected(category) },
                        onError = { /* sin modelo o sin permiso: sigue sin avisos */ },
                        onWindow = { scores ->
                            // Top-3 de la ventana, legible en pantalla: decide en una prueba
                            // si el clasificador oye (scores fluyen) o los umbrales bloquean.
                            val top = scores.entries.sortedByDescending { it.value }.take(3)
                                .joinToString(" · ") { "%s %.2f".format(it.key, it.value) }
                            listeningState.setLastWindow(top.ifEmpty { null })
                        }
                    )
                    this@SoundAlertService.classifier = classifier
                    classifier.start()
                }
            }.onFailure { e ->
                listeningState.setStopReason("clasificador: ${describeThrowable(e)}")
                stopSelf()
            }
        }
    }

    // NOT_STICKY: la escucha se (re)activa solo por accion del usuario. Un servicio de
    // microfono resucitado por el sistema sin contexto es el ingrediente del crash-loop.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Clase y mensaje del error Y de su cadena de causas (hasta 3 niveles). Un
     * ExceptionInInitializerError, por ejemplo, lleva la causa real en `cause` — sin
     * recorrerla, el diagnostico se queda en "null" (QA 14-ago, captura de Pablo).
     */
    private fun describeThrowable(e: Throwable): String {
        val parts = mutableListOf<String>()
        var t: Throwable? = e
        var depth = 0
        while (t != null && depth < 3) {
            parts.add("${t.javaClass.simpleName}: ${t.message ?: "(sin mensaje)"}")
            t = t.cause
            depth++
        }
        return parts.joinToString(" <- ")
    }

    override fun onDestroy() {
        listeningState.setListening(false)
        listeningState.setLastWindow(null)
        flashJob?.cancel()
        classifier?.stop()
        classifier = null
        runCatching { torch.turnOff() }
        scope.cancel()
        super.onDestroy()
    }

    private fun onDetected(category: SoundCategory) {
        listeningState.setLastDetection(getString(category.labelRes()))
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
        // ID PROPIO (QA 14-ago): antes usaba NOTIF_ID — el MISMO de la notificacion del
        // servicio en primer plano — y la MACHACABA en vez de crear una alerta nueva.
        // La deteccion es un evento puntual: autoCancel, no ongoing.
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sa_notif_detected))
            .setContentText(getString(category.labelRes()))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        mgr.notify(DETECTION_NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "sound_alert"
        private const val NOTIF_ID = 2
        private const val SCREEN_NOTIF_ID = 3
        private const val DETECTION_NOTIF_ID = 4

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
