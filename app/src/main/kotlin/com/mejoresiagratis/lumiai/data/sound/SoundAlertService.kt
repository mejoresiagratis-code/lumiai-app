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
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import com.mejoresiagratis.lumiai.domain.sound.SoundDetectionEngine

/**
 * Servicio en primer plano (tipo microfono) que escucha y clasifica sonidos en el dispositivo y
 * avisa cuando reconoce una categoria. En F2 el aviso es una notificacion; el destello de luz
 * llega en F3.
 *
 * La configuracion usa los valores por defecto (8 categorias activas); la persistencia en
 * DataStore tambien es de F3. Requiere RECORD_AUDIO concedido y el modelo yamnet.tflite en
 * assets: sin ellos el servicio sigue vivo pero no avisa (no se finge deteccion).
 */
class SoundAlertService : Service() {

    private var classifier: MediaPipeSoundClassifier? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startInForeground()

        val config = SoundAlertConfig()
        val engine = SoundDetectionEngine(config)
        val classifier = MediaPipeSoundClassifier(
            context = applicationContext,
            engine = engine,
            allowedLabels = config.activeLabels().toList(),
            onDetected = { category -> notifyDetection(category) },
            onError = { /* sin modelo o sin permiso: el servicio sigue sin avisos */ }
        )
        this.classifier = classifier
        classifier.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        classifier?.stop()
        classifier = null
        super.onDestroy()
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
