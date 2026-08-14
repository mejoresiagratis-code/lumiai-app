package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.mejoresiagratis.lumiai.domain.flash.SelfOffWindow
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class Camera2TorchController @Inject constructor(
    @ApplicationContext private val context: Context
) : TorchController {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val flashCameraId: String? by lazy { findFlashCamera() }

    override val hasFlash: Boolean get() = flashCameraId != null

    // Ultima vez que ESTE controlador apago la linterna por su cuenta (turnOff, o el
    // respaldo interno de pulseOff): ventana usada por el TorchCallback de mas abajo
    // para descartar sus propios apagados y quedarse solo con los externos.
    @Volatile private var lastSelfOffAtMs: Long = 0L

    private val _externalOffEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val externalOffEvents: Flow<Unit> = _externalOffEvents.asSharedFlow()

    init {
        // Registrado UNA vez, vive con el proceso (Singleton) — no requiere unregister.
        runCatching {
            cameraManager.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (enabled || cameraId != flashCameraId) return
                        val isOwn = SelfOffWindow.isOwnOff(lastSelfOffAtMs, SystemClock.elapsedRealtime())
                        if (!isOwn) _externalOffEvents.tryEmit(Unit)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    override val maxIntensityLevel: Int by lazy {
        val id = flashCameraId ?: return@lazy 1
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@lazy 1
        runCatching {
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        }.getOrDefault(1)
    }

    override fun turnOn(intensityLevel: Int) {
        val id = flashCameraId ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensityLevel > 1) {
                cameraManager.turnOnTorchWithStrengthLevel(id, scaleToDevice(intensityLevel))
            } else {
                cameraManager.setTorchMode(id, true)
            }
        }
    }

    override fun turnOff() {
        val id = flashCameraId ?: return
        lastSelfOffAtMs = SystemClock.elapsedRealtime()
        runCatching { cameraManager.setTorchMode(id, false) }
    }

    override fun pulseOff() {
        // REVERTIDO (QA 14-ago): el experimento de "apagar" bajando al nivel minimo
        // (v0.9.17) dejaba un resplandor residual que difuminaba el contraste on/off de
        // los patrones (SOS/Estrobo/Baliza/Morse/Musica) — fidelidad del patron gana a
        // la estetica de la notificacion del sistema de Samsung, que volvera a parpadear
        // al ritmo del flash (inevitable: la genera el SO con cada apagado real, sin
        // API). El metodo se conserva como gancho semantico "hueco intra-patron" por si
        // algun dia se afina por dispositivo. turnOff() marca la ventana de SelfOffWindow,
        // asi que la deteccion de apagados EXTERNOS sigue sin falsos positivos por pulso.
        turnOff()
    }

    private fun scaleToDevice(logical: Int): Int {
        val pct = logical.coerceIn(FlashSettings.MIN_INTENSITY, FlashSettings.MAX_INTENSITY) / 100f
        return (pct * maxIntensityLevel).roundToInt().coerceIn(1, maxIntensityLevel)
    }

    private fun findFlashCamera(): String? = runCatching {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()
}
