package com.mejoresiagratis.lumiai.data.torch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.data.system.ManufacturerInfo
import com.mejoresiagratis.lumiai.data.system.NotificationIds
import com.mejoresiagratis.lumiai.domain.flash.FlashEngine
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TorchService : Service() {

    @Inject lateinit var engine: FlashEngine
    @Inject lateinit var repo: FlashStateRepository
    @Inject lateinit var torch: TorchController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        scope.launch {
            // Solo on/off y cambio de modo relanzan la rutina; los ajustes los escucha el engine.
            combine(repo.isOn, repo.mode) { on, mode -> on to mode }
                .distinctUntilChanged()
                .collectLatest { (on, mode) ->
                    if (!on) {
                        stopSelf()
                    } else {
                        engine.play(mode, repo.settings)
                    }
                }
        }
        // Apagado EXTERNO (boton "Desactivar" de Samsung, u otra app): la misma
        // palanca que usa nuestro propio boton "Apagar" — repo.setOn(false) — hace el
        // resto solo (el colector de arriba para el motor y llama stopSelf()). Sin
        // esto, el motor seguia reencendiendo la luz en cada pulso de SOS/Estrobo y la
        // notificacion de Samsung "revivia" en cada ciclo (QA 13-ago).
        scope.launch {
            torch.externalOffEvents.collect { if (repo.isOn.value) repo.setOn(false) }
        }
        scope.launch { beaconAutoOff() }
    }

    /**
     * Auto-apagado de Baliza (22-ago). Vive AQUI, en el servicio que mantiene la luz encendida,
     * y se DERIVA del estado en vez de programarse a mano.
     *
     * La version anterior estaba en el ViewModel y acumulaba cinco defectos, todos por ser
     * imperativa: solo se programaba al pulsar encender —entrar en Baliza con la luz ya encendida
     * no lo activaba—, cambiar los minutos no lo reprogramaba, salir del modo no lo cancelaba
     * (asi que al vencer podia apagar OTRO modo), y moria con el ViewModel aunque el servicio
     * siguiera encendido.
     *
     * `collectLatest` resuelve los cinco de una vez: cualquier cambio en encendido, modo o
     * minutos CANCELA la espera pendiente y vuelve a evaluar. No hay nada que cancelar ni
     * reprogramar a mano porque no hay temporizador propio: hay una espera que solo existe
     * mientras las condiciones se cumplen.
     */
    private suspend fun beaconAutoOff() {
        combine(
            repo.isOn,
            repo.mode,
            repo.settings.map { it.beaconAutoOffMin }.distinctUntilChanged()
        ) { on, mode, minutes -> Triple(on, mode, minutes) }
            .distinctUntilChanged()
            .collectLatest { (on, mode, minutes) ->
                if (on && mode == FlashMode.BEACON && minutes > 0) {
                    delay(minutes * 60_000L)
                    // Releer el estado: entre la espera y ahora el usuario pudo apagar o cambiar
                    // de modo, y collectLatest ya lo habria cancelado — esto es el cinturon.
                    if (repo.isOn.value && repo.mode.first() == FlashMode.BEACON) {
                        repo.setOn(false)
                    }
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Boton "Apagar" de la notificacion: apagar el estado hace el resto (el colector
        // de onCreate detiene el engine y llama stopSelf). Un solo camino de apagado.
        if (intent?.action == ACTION_STOP) {
            repo.setOn(false)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TorchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.torch_running))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.notif_action_off), stop)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "com.mejoresiagratis.lumiai.action.TORCH_STOP"

        // Canal DISTINTO por fabricante (QA 13-ago): la importancia de un canal es
        // INMUTABLE una vez creado — no se puede "bajar" en caliente para instalaciones
        // ya existentes. Usar un ID nuevo evita colisionar con cualquier canal "torch"
        // previo. En Samsung, el sistema YA muestra su propia notificacion de "Linterna
        // encendida / Desactivar" (fuera de nuestro control, sin API para suprimirla):
        // la nuestra seria puro ruido duplicado, asi que nace en IMPORTANCE_MIN (sigue
        // existiendo — Android exige notificacion para todo servicio en primer plano —
        // pero sin icono en la barra de estado, "por debajo del pliegue" en el panel).
        // En el resto de fabricantes sigue en IMPORTANCE_LOW: es la unica fuente visible.
        private val CHANNEL_ID = if (ManufacturerInfo.isSamsung) "torch_samsung" else "torch"
        private const val NOTIF_ID = NotificationIds.TORCH

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(NotificationManager::class.java)
                if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                    val importance = if (ManufacturerInfo.isSamsung) {
                        NotificationManager.IMPORTANCE_MIN
                    } else {
                        NotificationManager.IMPORTANCE_LOW
                    }
                    mgr.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, "Torch", importance)
                    )
                }
            }
        }
    }
}
