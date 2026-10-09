package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class Camera2TorchControllerTest {
    private val context = mockk<Context>()
    private val manager = mockk<CameraManager>()
    private val pm = mockk<PackageManager>()
    private val characteristics = mockk<CameraCharacteristics>()
    private val callback = slot<CameraManager.TorchCallback>()
    private lateinit var torch: Camera2TorchController

    @Before fun setUp() {
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns manager
        every { context.packageManager } returns pm
        every { pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) } returns true
        every { manager.registerTorchCallback(capture(callback), any<Handler>()) } just Runs
        every { manager.cameraIdList } returns arrayOf("0")
        every { manager.getCameraCharacteristics("0") } returns characteristics
        every { characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns true
        every { manager.setTorchMode(any(), any()) } just Runs
        torch = Camera2TorchController(context)
    }

    @Test fun `busy failure is visible and next attempt recovers`() {
        every { manager.setTorchMode("0", true) } throws CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        assertEquals(TorchFailure.BUSY, torch.failure.value)
        every { manager.setTorchMode("0", true) } just Runs
        torch.turnOn(100)
        assertNull(torch.failure.value)
    }

    @Test fun `discovery failure does not disable hardware permanently`() {
        every { manager.cameraIdList } throws CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED)
        assertTrue(torch.hasFlash)
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        every { manager.cameraIdList } returns arrayOf("0")
        torch.turnOn(100)
        assertNull(torch.failure.value)
        verify(exactly = 1) { manager.setTorchMode("0", true) }
    }

    @Test fun `permission failure has actionable reason`() {
        every { manager.setTorchMode("0", true) } throws SecurityException()
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        assertEquals(TorchFailure.PERMISSION, torch.failure.value)
    }

    @Test fun `policy disabled is distinct from busy`() {
        every { manager.setTorchMode("0", true) } throws CameraAccessException(CameraAccessException.CAMERA_DISABLED)
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        assertEquals(TorchFailure.DISABLED, torch.failure.value)
    }

    @Test fun `availability loss stops active owner and permits retry`() = runTest {
        // Registration may immediately report unavailable: idle is not an error.
        callback.captured.onTorchModeUnavailable("0")
        assertNull(torch.failure.value)
        torch.turnOn(100)
        val stopped = async(start = CoroutineStart.UNDISPATCHED) { torch.externalOffEvents.first() }
        callback.captured.onTorchModeUnavailable("0")
        stopped.await()
        assertEquals(TorchFailure.UNAVAILABLE, torch.failure.value)
        torch.turnOn(100)
        assertNull(torch.failure.value)
    }

    @Test fun `cleanup failure never throws but remains visible and retryable`() {
        torch.turnOn(100)
        every { manager.setTorchMode("0", false) } throws CameraAccessException(CameraAccessException.CAMERA_ERROR)
        torch.turnOff()
        assertEquals(TorchFailure.OFF_FAILED, torch.failure.value)
        every { manager.setTorchMode("0", false) } just Runs
        torch.turnOff()
        verify(exactly = 2) { manager.setTorchMode("0", false) }
    }

    @Test fun `pulse off failure aborts the pattern`() {
        torch.turnOn(100)
        every { manager.setTorchMode("0", false) } throws CameraAccessException(CameraAccessException.CAMERA_ERROR)
        assertThrows(TorchOperationException::class.java) { torch.pulseOff() }
    }

    @Test fun `cleanup preserves original permission error`() {
        every { manager.setTorchMode("0", true) } throws SecurityException()
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        every { manager.setTorchMode("0", false) } throws SecurityException()
        torch.turnOff()
        assertEquals(TorchFailure.PERMISSION, torch.failure.value)
    }
    @Test @Config(sdk = [33])
    fun `strength capability is cached while device remains available`() {
        every { characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) } returns 5
        every { manager.turnOnTorchWithStrengthLevel("0", any()) } just Runs
        torch.turnOn(100)
        torch.pulseOff()
        torch.turnOn(20)
        verify(exactly = 1) { characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) }
        verify { manager.turnOnTorchWithStrengthLevel("0", 5) }
        verify { manager.turnOnTorchWithStrengthLevel("0", 1) }
    }

    @Test fun `missing camera can be discovered on a later attempt`() {
        every { manager.cameraIdList } returns emptyArray()
        assertThrows(TorchOperationException::class.java) { torch.turnOn(100) }
        assertEquals(TorchFailure.UNAVAILABLE, torch.failure.value)
        every { manager.cameraIdList } returns arrayOf("0")
        torch.turnOn(100)
        assertNull(torch.failure.value)
    }

}
