package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import dagger.hilt.android.qualifiers.ApplicationContext
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
        runCatching { cameraManager.setTorchMode(id, false) }
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
