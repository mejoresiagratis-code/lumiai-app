package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    @Volatile private var cachedCameraId: String? = null
    @Volatile private var strengthCache: Pair<String, Int>? = null
    @Volatile private var lastControlledId: String? = null
    @Volatile private var requestedOn = false
    private val _failure = MutableStateFlow<TorchFailure?>(null)
    override val failure = _failure.asStateFlow()

    // A transient CameraManager failure must never become a permanent "no flash" capability.
    override val hasFlash: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private fun cameraId(): String = cachedCameraId ?: findFlashCamera()?.also {
        cachedCameraId = it
    } ?: throw TorchOperationException(if (hasFlash) TorchFailure.UNAVAILABLE else TorchFailure.NO_FLASH)

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
                    override fun onTorchModeUnavailable(cameraId: String) {
                        if (cameraId != cachedCameraId || !requestedOn) return
                        requestedOn = false
                        strengthCache = null
                        cachedCameraId = null
                        lastControlledId = null
                        _failure.value = TorchFailure.UNAVAILABLE
                        _externalOffEvents.tryEmit(Unit)
                    }

                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (enabled || cameraId != cachedCameraId) return
                        val isOwn = SelfOffWindow.isOwnOff(lastSelfOffAtMs, SystemClock.elapsedRealtime())
                        if (!isOwn && requestedOn) {
                            requestedOn = false
                            _externalOffEvents.tryEmit(Unit)
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    override val maxIntensityLevel: Int
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1
            return runCatching {
                val id = cameraId()
                strengthCache?.takeIf { it.first == id }?.second ?: (
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    ).coerceAtLeast(1).also { strengthCache = id to it }
            }.getOrDefault(1).coerceAtLeast(1)
        }

    override fun turnOn(intensityLevel: Int) {
        try {
            val id = cameraId()
            val maximum = maxIntensityLevel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maximum > 1) {
                cameraManager.turnOnTorchWithStrengthLevel(id, scaleToDevice(intensityLevel, maximum))
            } else {
                cameraManager.setTorchMode(id, true)
            }
            lastControlledId = id
            requestedOn = true
            _failure.value = null
        } catch (e: Exception) {
            requestedOn = false
            strengthCache = null
            cachedCameraId = null // Rediscover on the next explicit start / sound alert.
            val reason = when (e) {
                is TorchOperationException -> e.failure
                is SecurityException -> TorchFailure.PERMISSION
                is CameraAccessException -> when (e.reason) {
                    CameraAccessException.CAMERA_IN_USE,
                    CameraAccessException.MAX_CAMERAS_IN_USE -> TorchFailure.BUSY
                    CameraAccessException.CAMERA_DISABLED -> TorchFailure.DISABLED
                    else -> TorchFailure.UNAVAILABLE
                }
                else -> TorchFailure.UNAVAILABLE
            }
            _failure.value = reason
            throw TorchOperationException(reason, e)
        }
    }

    // Cleanup must not throw and mask the original error or prevent session handover.
    override fun turnOff() {
        requestedOn = false
        val id = lastControlledId ?: return
        lastSelfOffAtMs = SystemClock.elapsedRealtime()
        try {
            cameraManager.setTorchMode(id, false)
            lastControlledId = null
        } catch (_: Exception) {
            if (_failure.value == null) _failure.value = TorchFailure.OFF_FAILED
        }
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
        if (_failure.value == TorchFailure.OFF_FAILED) {
            throw TorchOperationException(TorchFailure.OFF_FAILED)
        }
    }

    private fun scaleToDevice(logical: Int, maximum: Int): Int {
        val pct = logical.coerceIn(FlashSettings.MIN_INTENSITY, FlashSettings.MAX_INTENSITY) / 100f
        return (pct * maximum).roundToInt().coerceIn(1, maximum)
    }

    private fun findFlashCamera(): String? =
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

}
