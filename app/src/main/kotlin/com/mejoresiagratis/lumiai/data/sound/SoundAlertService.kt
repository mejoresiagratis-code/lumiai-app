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
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.data.session.HardwareSessionCoordinator
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.system.NotificationIds
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.entitlement.whileAiAccess
import com.mejoresiagratis.lumiai.domain.entitlement.ProAccessMonitor
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertConfigRepository
import com.mejoresiagratis.lumiai.domain.repository.SoundAlertStateRepository
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertFlash
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundDetectionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano (tipo microfono) que escucha y clasifica sonidos en el dispositivo y,
 * al reconocer una categoria activa, avisa segun su canal configurado: destello del LED (patron
 * por ritmo), parpadeo de pantalla (ScreenFlashActivity via full-screen-intent) o ambos. Si se
 * pidio flash pero el dispositivo no tiene, cae a pantalla para no dejar sin aviso.
 *
 * La configuracion se aplica EN VIVO (14-ago): cada cambio de categorias, sensibilidad o canal
 * reconstruye clasificador y motor sin que el usuario tenga que parar y volver a iniciar. Requiere
 * RECORD_AUDIO y el modelo yamnet.tflite en assets: sin ellos sigue vivo pero no avisa.
 */
@AndroidEntryPoint
class SoundAlertService : Service() {

    @Inject lateinit var sessions: HardwareSessionCoordinator
    @Inject lateinit var flashState: FlashStateRepository
    @Inject lateinit var configRepo: SoundAlertConfigRepository
    @Inject lateinit var listeningState: SoundAlertStateRepository
    @Inject lateinit var proAccess: ProAccessMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ready = false

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
        ready = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!ready) return START_NOT_STICKY
        scope.launch {
            try {
                sessions.runSession(
                    onStarted = {
                        flashState.setOn(false)
                        listeningState.setStopReason(null)
                    },
                    onFinished = {
                        listeningState.setListening(false)
                        listeningState.setLastWindow(null)
                    }
                ) { torch ->
                    proAccess.hasAiAccess.whileAiAccess(
                        onDenied = { listeningState.setStopReason(getString(R.string.sa_stopped_no_pro)) }
                    ) {
                        listeningState.setListening(true)
                        configRepo.config.collectLatest { config ->
                            listen(config, torch, startId)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                listeningState.setStopReason("clasificador: ${describeThrowable(e)}")
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun listen(config: SoundAlertConfig, torch: TorchController, startId: Int): Unit = coroutineScope {
        // All callbacks hand off to session children. null cancels only the current flash.
        val events = Channel<SoundCategory?>(Channel.CONFLATED)
        val lastResultAt = AtomicLong(SystemClock.elapsedRealtime())
        listeningState.setLastWindow(getString(R.string.sa_analysis_starting))
        launch {
            while (isActive) {
                delay(1_000)
                if (SystemClock.elapsedRealtime() - lastResultAt.get() >= 15_000) {
                    listeningState.setStopReason(getString(R.string.sa_analysis_timeout))
                    stopSelf(startId)
                    break
                }
            }
        }
        launch {
            events.receiveAsFlow().collectLatest { category ->
                if (category != null) onDetected(category, config, torch)
            }
        }
        launch { torch.externalOffEvents.collect { events.trySend(null) } }
        val classifier = MediaPipeSoundClassifier(
            context = applicationContext,
            engine = SoundDetectionEngine(config),
            onDetected = { category -> if (isActive) events.trySend(category) },
            onError = { reason ->
                if (isActive) {
                    listeningState.setStopReason(reason)
                    stopSelf(startId)
                }
            },
            onWindow = { scores, levelDb, count ->
                if (isActive) {
                    lastResultAt.set(SystemClock.elapsedRealtime())
                    val top = scores.entries.sortedByDescending { it.value }.take(3)
                        .joinToString(" · ") { "%s %.2f".format(it.key, it.value) }
                    listeningState.setLastWindow(getString(
                        R.string.sa_analysis_result, count, levelDb.toInt(),
                        top.ifEmpty { getString(R.string.sa_analysis_empty) }
                    ))
                }
            }
        )
        try {
            classifier.start()
            awaitCancellation()
        } finally {
            events.close()
            classifier.stop()
        }
    }

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
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun onDetected(category: SoundCategory, config: SoundAlertConfig, torch: TorchController) {
        listeningState.setLastDetection(getString(category.labelRes()))
        notifyDetection(category)
        val channel = config.channel(category)
        var useFlash = channel.usesFlash && torch.hasFlash
        val wantsScreen = channel.usesScreen || (channel.usesFlash && !useFlash)
        if (wantsScreen) {
            if (canUseFullScreenAlerts()) {
                screenFlash(category)
            } else if (!useFlash && torch.hasFlash) {
                useFlash = true
            } else {
                listeningState.setStopReason(getString(R.string.sa_screen_alert_blocked))
            }
        }
        if (useFlash) flash(category, torch)
    }

    /**
     * ¿Puede la app mostrar avisos a pantalla completa? (22-ago)
     *
     * Android 14 restringio este permiso: se concede solo a apps de alarma y llamadas, y el
     * usuario puede revocarlo. Sin comprobarlo, el canal "solo pantalla" prometia un aviso que
     * podia no aparecer NUNCA — justo en la funcion pensada para quien no oye.
     */
    private fun canUseFullScreenAlerts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val mgr = getSystemService(NotificationManager::class.java) ?: return false
        return runCatching { mgr.canUseFullScreenIntent() }.getOrDefault(false)
    }

    /**
     * Aviso de pantalla. Se emite por DOS vias porque ninguna cubre todos los casos (QA 22-ago):
     *
     * - **Intent a pantalla completa**: es la unica via permitida para "despertar" el movil, y
     *   funciona con la pantalla APAGADA o bloqueada — el escenario principal de esta funcion.
     *   Pero con el movil desbloqueado y en uso, Android lo degrada DELIBERADAMENTE a una
     *   notificacion flotante que hay que tocar. No es un fallo nuestro: es la regla del sistema.
     * - **Arranque directo de la actividad**: solo se permite si la app esta en primer plano, y
     *   cubre justo el hueco anterior. Se intenta ademas de la notificacion, no en su lugar.
     */
    private fun screenFlash(category: SoundCategory) {
        val intent = Intent(this, ScreenFlashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenFlashActivity.EXTRA_PATTERN, SoundAlertFlash.patternFor(category))
        }
        // Con la app visible, el arranque directo SI esta permitido y el aviso aparece al
        // instante, sin tocar nada. Si la app esta en segundo plano el sistema lo ignora en
        // silencio (no lanza), y queda la notificacion de abajo como via.
        runCatching { startActivity(intent) }
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
            .setContentIntent(SoundAlertNotifications.open(this))
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(SCREEN_NOTIF_ID, notif)
    }

    private suspend fun flash(category: SoundCategory, torch: TorchController) {
        val pattern = SoundAlertFlash.patternFor(category)
        try {
            var i = 0
            while (i < pattern.size) {
                torch.turnOn(FlashSettings.MAX_INTENSITY)
                delay(pattern[i])
                torch.pulseOff()
                if (i + 1 < pattern.size) delay(pattern[i + 1])
                i += 2
            }
        } finally {
            torch.turnOff()
        }
    }

    private fun startInForeground() {
        val notif = SoundAlertNotifications.listening(this)
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
            .setContentIntent(SoundAlertNotifications.open(this))
            .setAutoCancel(true)
            .build()
        mgr.notify(DETECTION_NOTIF_ID, notif)
    }

    companion object {
        const val ACTION_STOP = "com.mejoresiagratis.lumiai.action.SOUND_ALERT_STOP"
        private const val CHANNEL_ID = "sound_alert"
        // IDs centralizados (17-ago): DETECTION_NOTIF_ID valia 4 y CHOCABA con la
        // notificacion de primer plano de MusicFlashService — al detectar un sonido con
        // Musica activa, la borraba. Ver NotificationIds.
        private const val NOTIF_ID = NotificationIds.SOUND_ALERT_FOREGROUND
        private const val SCREEN_NOTIF_ID = NotificationIds.SOUND_ALERT_SCREEN
        private const val DETECTION_NOTIF_ID = NotificationIds.SOUND_ALERT_DETECTION

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
                mgr.createNotificationChannel(NotificationChannel(
                    SoundAlertNotifications.LISTENING_CHANNEL,
                    context.getString(R.string.sa_title),
                    NotificationManager.IMPORTANCE_LOW
                ))
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
