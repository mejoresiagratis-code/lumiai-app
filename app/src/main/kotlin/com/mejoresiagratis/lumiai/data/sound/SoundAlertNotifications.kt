package com.mejoresiagratis.lumiai.data.sound

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mejoresiagratis.lumiai.MainActivity
import com.mejoresiagratis.lumiai.R

internal object SoundAlertNotifications {
    const val ALERT_CHANNEL = "sound_alert"
    const val LISTENING_CHANNEL = "sound_alert_listening"

    fun open(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 100,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_SOUND_ALERT)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun screen(context: Context, category: com.mejoresiagratis.lumiai.domain.sound.SoundCategory,
               fullScreenAllowed: Boolean): android.app.Notification {
        val pending = PendingIntent.getActivity(
            context, 200 + category.ordinal,
            ScreenFlashActivity.createIntent(context,
                com.mejoresiagratis.lumiai.domain.sound.SoundAlertFlash.patternFor(category)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setContentTitle(context.getString(R.string.sa_notif_detected))
            .setContentText(context.getString(R.string.sa_screen_tap, context.getString(category.labelRes())))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setAutoCancel(true)
        if (fullScreenAllowed) builder.setFullScreenIntent(pending, true)
        return builder.build()
    }

    fun listening(context: Context) = NotificationCompat.Builder(context, LISTENING_CHANNEL)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.sa_notif_listening))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(open(context))
        .addAction(0, context.getString(R.string.sa_stop), PendingIntent.getService(
            context, 101,
            Intent(context, SoundAlertService::class.java).setAction(SoundAlertService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .build()
}
