package com.mejoresiagratis.lumiai.data.sound

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Parpadea la pantalla a pantalla completa siguiendo un patron (pares on/off en ms) y se cierra
 * sola. Es la via de aviso "Pantalla" del modo Alerta Sonora, util cuando el movil no tiene flash
 * o ademas de el. Se muestra sobre la pantalla de bloqueo y enciende la pantalla.
 *
 * El servicio la lanza mediante una notificacion con full-screen-intent. En Android 14+ el
 * full-screen-intent puede degradarse a notificacion expandida si el sistema no concede el
 * permiso a la app; en ese caso el aviso sigue llegando como heads-up (no se finge nada).
 */
class ScreenFlashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val surface = View(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(surface)

        val pattern = intent.getLongArrayExtra(EXTRA_PATTERN)?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_PATTERN

        lifecycleScope.launch {
            try {
                repeat(CYCLES) {
                    var i = 0
                    while (i < pattern.size) {
                        surface.setBackgroundColor(Color.WHITE)
                        delay(pattern[i])
                        surface.setBackgroundColor(Color.BLACK)
                        if (i + 1 < pattern.size) delay(pattern[i + 1])
                        i += 2
                    }
                    delay(GAP_MS)
                }
            } finally {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_PATTERN = "extra_pattern"
        private const val CYCLES = 2
        private const val GAP_MS = 250L
        private val DEFAULT_PATTERN = longArrayOf(300, 200, 300, 200)
    }
}
