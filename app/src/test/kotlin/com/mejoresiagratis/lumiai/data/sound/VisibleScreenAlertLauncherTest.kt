package com.mejoresiagratis.lumiai.data.sound

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class VisibleScreenAlertLauncherTest {
    @Test fun visibleActivityCanShowScreenWithoutFullScreenNotificationGate() {
        val owner = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val launcher = VisibleScreenAlertLauncher()
            val activity = owner.get()
            shadowOf(activity.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
            launcher.onActivityStarted(activity)
            assertTrue(launcher.show(ScreenFlashActivity.createIntent(activity)))
            assertEquals(ScreenFlashActivity::class.java.name,
                shadowOf(activity).nextStartedActivity.component?.className)
        } finally { owner.pause().stop().destroy() }
    }

    @Test fun stoppedActivityDoesNotLaunchFromBackground() {
        val owner = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val launcher = VisibleScreenAlertLauncher()
            val activity = owner.get()
            launcher.onActivityStarted(activity)
            launcher.onActivityStopped(activity)
            assertFalse(launcher.show(ScreenFlashActivity.createIntent(activity)))
            assertNull(shadowOf(activity).nextStartedActivity)
        } finally { owner.pause().stop().destroy() }
    }

    @Test fun lockedDeviceUsesNotificationPath() {
        val owner = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val launcher = VisibleScreenAlertLauncher()
            val activity = owner.get()
            shadowOf(activity.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
            launcher.onActivityStarted(activity)
            assertFalse(launcher.show(ScreenFlashActivity.createIntent(activity)))
            assertNull(shadowOf(activity).nextStartedActivity)
        } finally { owner.pause().stop().destroy() }
    }

    @Test fun noActivityDoesNotLaunchFromServiceContext() {
        assertFalse(VisibleScreenAlertLauncher().show(
            ScreenFlashActivity.createIntent(RuntimeEnvironment.getApplication())
        ))
    }
}
