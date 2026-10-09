package com.mejoresiagratis.lumiai.data.sound

import android.app.Application
import android.app.Notification
import android.content.Intent
import com.mejoresiagratis.lumiai.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SoundAlertNotificationsTest {
    @Test fun tappingListeningNotificationOpensSoundAlert() {
        val notification = SoundAlertNotifications.listening(RuntimeEnvironment.getApplication())
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(MainActivity.ACTION_OPEN_SOUND_ALERT, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(notification.contentIntent.isImmutable)
    }

    @Test fun deniedFullscreenStillOpensActualScreenWhenTapped() {
        val notification = SoundAlertNotifications.screen(
            RuntimeEnvironment.getApplication(),
            com.mejoresiagratis.lumiai.domain.sound.SoundCategory.TIMBRE, false
        )
        assertNull(notification.fullScreenIntent)
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(ScreenFlashActivity::class.java.name, intent.component?.className)
        assertTrue(intent.hasExtra(ScreenFlashActivity.EXTRA_PATTERN))
    }

    @Test fun allowedFullscreenAndTapUseTheSameScreenIntent() {
        val notification = SoundAlertNotifications.screen(
            RuntimeEnvironment.getApplication(),
            com.mejoresiagratis.lumiai.domain.sound.SoundCategory.TIMBRE, true
        )
        assertNotNull(notification.fullScreenIntent)
        assertEquals(notification.contentIntent, notification.fullScreenIntent)
    }

    @Test fun listeningHasStopActionAndSeparateQuietChannel() {
        val notification = SoundAlertNotifications.listening(RuntimeEnvironment.getApplication())
        assertEquals(SoundAlertNotifications.LISTENING_CHANNEL, notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        val stop = notification.actions.single().actionIntent
        val intent = shadowOf(stop).savedIntent
        assertEquals(SoundAlertService::class.java.name, intent.component?.className)
        assertEquals(SoundAlertService.ACTION_STOP, intent.action)
        assertTrue(stop.isImmutable)
    }
}
