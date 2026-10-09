package com.mejoresiagratis.lumiai.data.sound

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.annotation.MainThread
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/** Only a visible Activity starts the screen directly. Background delivery uses notifications. */
@Singleton
class VisibleScreenAlertLauncher @Inject constructor() : Application.ActivityLifecycleCallbacks {
    private val visible = mutableListOf<WeakReference<Activity>>()

    @MainThread
    fun show(intent: Intent): Boolean {
        val host = visible.asReversed().mapNotNull { it.get() }
            .firstOrNull { !it.isFinishing && !it.isDestroyed } ?: return false
        if (host.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) return false
        return runCatching { host.startActivity(intent); true }.getOrDefault(false)
    }

    override fun onActivityStarted(activity: Activity) {
        visible.removeAll { it.get() == null || it.get() === activity }
        visible += WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        visible.removeAll { it.get() == null || it.get() === activity }
    }

    override fun onActivityDestroyed(activity: Activity) = onActivityStopped(activity)
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
}
