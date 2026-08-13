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
import com.mejoresiagratis.lumiai.domain.flash.FlashEngine
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TorchService : Service() {

    @Inject lateinit var engine: FlashEngine
    @Inject lateinit var repo: FlashStateRepository

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
        private const val CHANNEL_ID = "torch"
        private const val NOTIF_ID = 1

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(NotificationManager::class.java)
                if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                    mgr.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, "Torch", NotificationManager.IMPORTANCE_LOW)
                    )
                }
            }
        }
    }
}
