package com.mejoresiagratis.lumiai.data.sound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
 * al reconocer una categoria activa, avisa con un destello del LED (patron por ritmo) y una
 * notificacion.
 *
 * Lee la configuracion persistida (DataStore) al arrancar; los cambios de categorias/sensibilidad
 * se aplican al reiniciar la escucha (el clasificador no se reconstruye en vivo). Requiere
 * RECORD_AUDIO y el modelo yamnet.tflite en assets: sin ellos sigue vivo pero no avisa.
 *
 * Caida a pantalla (cuando no hay flash) queda pendiente: si no hay LED, no destella aun.
 */
@AndroidEntryPoint
class SoundAlertService : Service() {

    @Inject lateinit var torch: TorchController
    @Inject lateinit var configRepo: SoundAlertConfigRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var classifier: MediaPipeSoundClassifier? = null
    private var flashJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startInForeground()
        scope.launch {
            val config = configRepo.config.first()
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
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        flashJob?.cancel()
        classifier?.stop()
        classifier = null
        runCatching { torch.turnOff() }
        scope.cancel()
        super.onDestroy()
    }

    private fun onDetected(category: SoundCategory) {
        notifyDetection(category)
        flash(category)
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
                    torch.turnOff()
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
            .setContentText("Escuchando alertas sonoras…")
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
            .setContentTitle("Sonido detectado")
            .setContentText(category.shortLabel())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        mgr.notify(NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "sound_alert"
        private const val NOTIF_ID = 2

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
                            "Alerta sonora",
                            NotificationManager.IMPORTANCE_HIGH
                        )
                    )
                }
            }
        }
    }
}

private fun SoundCategory.shortLabel(): String = when (this) {
    SoundCategory.TIMBRE -> "Timbre"
    SoundCategory.GOLPES_PUERTA -> "Golpes en la puerta"
    SoundCategory.TELEFONO -> "Teléfono"
    SoundCategory.PERRO -> "Perro"
    SoundCategory.BEBE -> "Llanto de bebé"
    SoundCategory.DESPERTADOR -> "Despertador / alarma"
    SoundCategory.SIRENA -> "Sirena"
    SoundCategory.ALARMA_HUMO -> "Alarma de humo/incendio"
}
