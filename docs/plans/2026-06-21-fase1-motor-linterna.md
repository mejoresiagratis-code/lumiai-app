# Fase 1 — Motor de linterna + 4 modos básicos · Implementation Plan

> **Para ejecución agéntica:** este plan se implementa tarea a tarea. Los pasos usan checkbox (`- [ ]`).
> Cada tarea deja el proyecto **compilando** y, donde aplica, con **tests verdes**.

**Goal:** Una linterna 100% funcional y gratuita con cuatro modos (Continuo, Pantalla, SOS/Morse, Estroboscópico), ajustes que se aplican **al instante**, **sin conflictos** al cambiar de modo o de ajuste, persistencia con DataStore y supervivencia en segundo plano vía foreground service.

**Architecture:** Por capas (`domain` / `data` / `ui`). **Regla de oro:** una sola clase (`Camera2TorchController`) toca el LED por hardware. El `FlashEngine` ejecuta el modo dentro de una corrutina y se conduce desde el estado reactivo con `collectLatest`, de modo que cada cambio de modo/ajuste **cancela** la rutina anterior y arranca la nueva → cero solapamientos, re-aplicación inmediata. El modo Pantalla no usa el LED (lo dibuja la UI).

**Tech Stack:** Kotlin 2.0.21 · Coroutines/Flow · Hilt · DataStore · Jetpack Compose (Material 3) · camera2 `CameraManager` · JUnit4 + MockK + Turbine + coroutines-test.

---

## File Structure

```
app/src/main/kotlin/com/mejoresiagratis/lumiai/
├── domain/
│   ├── model/
│   │   ├── FlashMode.kt              # enum de 4 modos
│   │   └── FlashSettings.kt          # ajustes (intensidad, estrobo, morse, color pantalla)
│   ├── flash/
│   │   ├── Morse.kt                  # texto -> duraciones on/off (puro)
│   │   ├── FlashEngine.kt            # ejecuta un modo (suspend, cancelable)
│   │   └── EngineController.kt       # interfaz start()/stop() (arranca/para el servicio)
│   └── repository/
│       └── FlashStateRepository.kt   # interfaz: isOn/mode/settings + setters
├── data/
│   ├── torch/
│   │   ├── TorchController.kt        # interfaz hardware LED
│   │   ├── Camera2TorchController.kt # ÚNICA clase que toca el LED
│   │   ├── TorchService.kt           # foreground service que conduce el engine
│   │   └── ServiceEngineController.kt# impl de EngineController (start/stop service)
│   └── settings/
│       ├── FlashSettingsMapper.kt    # settings <-> Preferences (puro)
│       └── DataStoreFlashStateRepository.kt
├── di/
│   └── AppModule.kt                  # bindings Hilt
└── ui/
    ├── home/
    │   ├── FlashUiState.kt
    │   ├── FlashViewModel.kt
    │   ├── HomeScreen.kt
    │   └── components/
    │       ├── ModeSelector.kt
    │       ├── ModeSettingsPanel.kt
    │       └── ScreenLight.kt        # overlay blanco para modo Pantalla
    └── navigation/LumiAiNavHost.kt   # (modificar) HomeScreen ya enrutado

app/src/test/kotlin/com/mejoresiagratis/lumiai/
├── domain/flash/MorseTest.kt
├── domain/flash/FlashEngineTest.kt
├── data/settings/FlashSettingsMapperTest.kt
├── ui/home/FlashViewModelTest.kt
└── util/MainDispatcherRule.kt        # + FakeTorchController, FakeFlashStateRepository
```

Convención de tests Android-puros: las clases sin dependencias de framework (Morse, FlashEngine, mapper, ViewModel) van con **TDD real**. Las clases atadas a Android (Camera2TorchController, TorchService, Compose) se entregan con **código + verificación de build + checklist manual on-device** (la prueba instrumentada llega en la fase de tests, P2 del roadmap global).

---

## Task 1: Modelos de dominio

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/model/FlashMode.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/model/FlashSettings.kt`
- Test: `app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/model/FlashSettingsTest.kt`

- [ ] **Step 1: Test que falla (coerción de ajustes)**

```kotlin
package com.mejoresiagratis.lumiai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashSettingsTest {
    @Test
    fun `coerced clamps every field into range`() {
        val s = FlashSettings(intensityLevel = 999, strobeHz = 99f, morseUnitMs = 5L).coerced()
        assertEquals(FlashSettings.MAX_INTENSITY, s.intensityLevel)
        assertEquals(FlashSettings.MAX_STROBE_HZ, s.strobeHz, 0f)
        assertEquals(FlashSettings.MIN_UNIT_MS, s.morseUnitMs)
    }
}
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashSettingsTest*"`
Expected: FAIL (clase `FlashSettings` no existe).

- [ ] **Step 3: Implementación mínima**

```kotlin
// FlashMode.kt
package com.mejoresiagratis.lumiai.domain.model

enum class FlashMode { CONTINUOUS, SCREEN, SOS_MORSE, STROBE }
```

```kotlin
// FlashSettings.kt
package com.mejoresiagratis.lumiai.domain.model

data class FlashSettings(
    val intensityLevel: Int = MAX_INTENSITY,   // lógico 1..100, el controller lo mapea al hardware
    val strobeHz: Float = 8f,
    val morseUnitMs: Long = 200L,
    val screenArgb: Int = -0x1                  // 0xFFFFFFFF (blanco)
) {
    fun coerced() = copy(
        intensityLevel = intensityLevel.coerceIn(MIN_INTENSITY, MAX_INTENSITY),
        strobeHz = strobeHz.coerceIn(MIN_STROBE_HZ, MAX_STROBE_HZ),
        morseUnitMs = morseUnitMs.coerceIn(MIN_UNIT_MS, MAX_UNIT_MS)
    )

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 100
        const val MIN_STROBE_HZ = 1f
        const val MAX_STROBE_HZ = 20f
        const val MIN_UNIT_MS = 60L
        const val MAX_UNIT_MS = 400L
    }
}
```

- [ ] **Step 4: Test verde**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashSettingsTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/model app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/model
git commit -m "feat(domain): FlashMode + FlashSettings with range coercion"
```

---

## Task 2: Codificador Morse (puro)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/Morse.kt`
- Test: `app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/flash/MorseTest.kt`

- [ ] **Step 1: Test que falla (SOS exacto)**

```kotlin
package com.mejoresiagratis.lumiai.domain.flash

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MorseTest {
    @Test
    fun `sos produces the exact international timing for unit 100ms`() {
        // S(...) gap O(---) gap S(...) — dot=1u, dash=3u, intra=1u(off), inter=3u(off)
        val expected = longArrayOf(
            100, 100, 100, 100, 100,   // S: . . .
            300,                       // inter-char gap
            300, 100, 300, 100, 300,   // O: - - -
            300,                       // inter-char gap
            100, 100, 100, 100, 100    // S: . . .
        )
        assertArrayEquals(expected, Morse.sos(100))
    }

    @Test
    fun `durations alternate starting with an ON element`() {
        val d = Morse.sos(100)
        // índices pares = ON, impares = OFF; debe terminar en ON
        assertArrayEquals(longArrayOf(100, 100, 100, 100, 100), longArrayOf(d[0], d[2], d[4], d[12], d[16]))
    }
}
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*MorseTest*"`
Expected: FAIL (`Morse` no existe).

- [ ] **Step 3: Implementación**

```kotlin
package com.mejoresiagratis.lumiai.domain.flash

/**
 * Convierte texto a una lista plana de duraciones on/off (ms) según timing Morse internacional:
 * dot=1u, dash=3u, hueco intra-carácter=1u, hueco inter-carácter=3u, hueco entre palabras=7u.
 * Índices pares = ON, impares = OFF. La lista empieza y termina en ON.
 */
object Morse {
    private val TABLE: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )

    fun toDurations(text: String, unitMs: Long): LongArray {
        val u = unitMs
        val out = ArrayList<Long>()
        var firstChar = true
        for (raw in text.uppercase()) {
            if (raw == ' ') {
                if (out.isNotEmpty()) out.add(7 * u) // hueco entre palabras (OFF)
                firstChar = true
                continue
            }
            val code = TABLE[raw] ?: continue
            if (!firstChar) out.add(3 * u)           // hueco inter-carácter (OFF)
            firstChar = false
            for ((i, sym) in code.withIndex()) {
                out.add(if (sym == '-') 3 * u else u) // ON
                if (i != code.lastIndex) out.add(u)   // hueco intra-carácter (OFF)
            }
        }
        return out.toLongArray()
    }

    fun sos(unitMs: Long): LongArray = toDurations("SOS", unitMs)
}
```

- [ ] **Step 4: Test verde**

Run: `./gradlew :app:testDebugUnitTest --tests "*MorseTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/Morse.kt app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/flash/MorseTest.kt
git commit -m "feat(flash): Morse encoder with international timing"
```

---

## Task 3: TorchController (interfaz + impl camera2 + fake de test)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/TorchController.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/Camera2TorchController.kt`
- Create: `app/src/test/kotlin/com/mejoresiagratis/lumiai/util/FakeTorchController.kt`

- [ ] **Step 1: Interfaz**

```kotlin
// TorchController.kt
package com.mejoresiagratis.lumiai.data.torch

/** Única abstracción que controla el LED por hardware. */
interface TorchController {
    val hasFlash: Boolean
    val maxIntensityLevel: Int       // 1 si el dispositivo no soporta control de intensidad
    fun turnOn(intensityLevel: Int)  // intensityLevel lógico 1..100
    fun turnOff()
}
```

- [ ] **Step 2: Implementación camera2 (la ÚNICA que toca el LED)**

```kotlin
// Camera2TorchController.kt
package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class Camera2TorchController @Inject constructor(
    private val context: Context
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
```

- [ ] **Step 3: Fake para tests**

```kotlin
// FakeTorchController.kt  (en src/test)
package com.mejoresiagratis.lumiai.util

import com.mejoresiagratis.lumiai.data.torch.TorchController

class FakeTorchController(
    override val hasFlash: Boolean = true,
    override val maxIntensityLevel: Int = 100
) : TorchController {
    var isOn: Boolean = false; private set
    var lastIntensity: Int = 0; private set
    val transitions = mutableListOf<Boolean>()   // historial de on/off

    override fun turnOn(intensityLevel: Int) {
        lastIntensity = intensityLevel
        if (!isOn) transitions.add(true)
        isOn = true
    }
    override fun turnOff() {
        if (isOn) transitions.add(false)
        isOn = false
    }
}
```

- [ ] **Step 4: Verificar build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/TorchController.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/Camera2TorchController.kt app/src/test/kotlin/com/mejoresiagratis/lumiai/util/FakeTorchController.kt
git commit -m "feat(torch): single Camera2TorchController + test fake"
```

---

## Task 4: FlashEngine (núcleo, TDD con tiempo virtual)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/FlashEngine.kt`
- Test: `app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/flash/FlashEngineTest.kt`

- [ ] **Step 1: Tests que fallan (estrobo, morse, continuo, cancelación→off)**

```kotlin
package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.util.FakeTorchController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashEngineTest {

    @Test
    fun `continuous turns torch on immediately`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, FlashSettings()) }
        runCurrent()
        assertTrue(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `cancelling play always leaves the torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.CONTINUOUS, FlashSettings()) }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(torch.isOn)
    }

    @Test
    fun `strobe toggles at the configured frequency`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        // 10 Hz -> periodo 100ms (on 50 / off 50)
        val job = launch { engine.play(FlashMode.STROBE, FlashSettings(strobeHz = 10f)) }
        runCurrent();           assertTrue(torch.isOn)
        advanceTimeBy(50); runCurrent(); assertFalse(torch.isOn)
        advanceTimeBy(50); runCurrent(); assertTrue(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `screen mode keeps the hardware torch off`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SCREEN, FlashSettings()) }
        runCurrent()
        assertFalse(torch.isOn)
        job.cancelAndJoin()
    }

    @Test
    fun `sos first three flashes are short`() = runTest {
        val torch = FakeTorchController()
        val engine = FlashEngine(torch)
        val job = launch { engine.play(FlashMode.SOS_MORSE, FlashSettings(morseUnitMs = 100L)) }
        runCurrent(); assertTrue(torch.isOn)            // primer punto ON
        advanceTimeBy(100); runCurrent(); assertFalse(torch.isOn) // off tras 1u
        advanceTimeBy(100); runCurrent(); assertTrue(torch.isOn)  // segundo punto ON
        job.cancelAndJoin()
        // tres encendidos cortos al menos
        assertEquals(true, torch.transitions.take(2).all { it == true || it == false })
    }
}
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashEngineTest*"`
Expected: FAIL (`FlashEngine` no existe).

- [ ] **Step 3: Implementación**

```kotlin
package com.mejoresiagratis.lumiai.domain.flash

import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Ejecuta un modo. Es `suspend` y cancelable: al cancelarse, apaga el LED. */
@Singleton
class FlashEngine @Inject constructor(
    private val torch: TorchController
) {
    suspend fun play(mode: FlashMode, settings: FlashSettings) {
        val s = settings.coerced()
        try {
            when (mode) {
                FlashMode.CONTINUOUS -> { torch.turnOn(s.intensityLevel); awaitCancellation() }
                FlashMode.SCREEN -> { torch.turnOff(); awaitCancellation() } // la pantalla la dibuja la UI
                FlashMode.STROBE -> strobe(s)
                FlashMode.SOS_MORSE -> morse(s)
            }
        } finally {
            torch.turnOff()
        }
    }

    private suspend fun strobe(s: FlashSettings) {
        val period = (1000f / s.strobeHz).toLong().coerceAtLeast(2L)
        val onMs = period / 2
        val offMs = period - onMs
        while (true) {
            torch.turnOn(s.intensityLevel); delay(onMs)
            torch.turnOff(); delay(offMs)
        }
    }

    private suspend fun morse(s: FlashSettings) {
        val durations = Morse.sos(s.morseUnitMs)
        while (true) {
            for (i in durations.indices) {
                if (i % 2 == 0) torch.turnOn(s.intensityLevel) else torch.turnOff()
                delay(durations[i])
            }
            torch.turnOff()
            delay(s.morseUnitMs * 7) // hueco entre repeticiones
        }
    }
}
```

- [ ] **Step 4: Test verde**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashEngineTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/FlashEngine.kt app/src/test/kotlin/com/mejoresiagratis/lumiai/domain/flash/FlashEngineTest.kt
git commit -m "feat(flash): FlashEngine driving all four modes (TDD)"
```

---

## Task 5: Persistencia (mapper puro + DataStore repo + fake)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/repository/FlashStateRepository.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/settings/FlashSettingsMapper.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/settings/DataStoreFlashStateRepository.kt`
- Create: `app/src/test/kotlin/com/mejoresiagratis/lumiai/util/FakeFlashStateRepository.kt`
- Test: `app/src/test/kotlin/com/mejoresiagratis/lumiai/data/settings/FlashSettingsMapperTest.kt`

- [ ] **Step 1: Interfaz del repositorio**

```kotlin
// FlashStateRepository.kt
package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FlashStateRepository {
    val isOn: StateFlow<Boolean>          // runtime (no persistido)
    val mode: Flow<FlashMode>             // persistido
    val settings: Flow<FlashSettings>     // persistido
    fun setOn(on: Boolean)
    suspend fun setMode(mode: FlashMode)
    suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings)
}
```

- [ ] **Step 2: Test que falla (mapper round-trip)**

```kotlin
package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashSettingsMapperTest {
    @Test
    fun `map round-trips settings through a plain map`() {
        val original = FlashSettings(intensityLevel = 60, strobeHz = 12f, morseUnitMs = 150L, screenArgb = -0x10000)
        val restored = FlashSettingsMapper.fromMap(FlashSettingsMapper.toMap(original))
        assertEquals(original, restored)
    }

    @Test
    fun `fromMap falls back to defaults on missing keys`() {
        assertEquals(FlashSettings(), FlashSettingsMapper.fromMap(emptyMap()))
    }
}
```

- [ ] **Step 3: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashSettingsMapperTest*"`
Expected: FAIL (`FlashSettingsMapper` no existe).

- [ ] **Step 4: Implementación del mapper (puro)**

```kotlin
// FlashSettingsMapper.kt
package com.mejoresiagratis.lumiai.data.settings

import com.mejoresiagratis.lumiai.domain.model.FlashSettings

object FlashSettingsMapper {
    const val KEY_INTENSITY = "intensity"
    const val KEY_STROBE_HZ = "strobe_hz"
    const val KEY_MORSE_UNIT = "morse_unit"
    const val KEY_SCREEN_ARGB = "screen_argb"

    fun toMap(s: FlashSettings): Map<String, Any> = mapOf(
        KEY_INTENSITY to s.intensityLevel,
        KEY_STROBE_HZ to s.strobeHz,
        KEY_MORSE_UNIT to s.morseUnitMs,
        KEY_SCREEN_ARGB to s.screenArgb
    )

    fun fromMap(m: Map<String, Any?>): FlashSettings {
        val d = FlashSettings()
        return FlashSettings(
            intensityLevel = (m[KEY_INTENSITY] as? Int) ?: d.intensityLevel,
            strobeHz = (m[KEY_STROBE_HZ] as? Float) ?: d.strobeHz,
            morseUnitMs = (m[KEY_MORSE_UNIT] as? Long) ?: d.morseUnitMs,
            screenArgb = (m[KEY_SCREEN_ARGB] as? Int) ?: d.screenArgb
        ).coerced()
    }
}
```

- [ ] **Step 5: Test verde**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashSettingsMapperTest*"`
Expected: PASS.

- [ ] **Step 6: Implementación DataStore + fake**

```kotlin
// DataStoreFlashStateRepository.kt
package com.mejoresiagratis.lumiai.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumiai_flash")

@Singleton
class DataStoreFlashStateRepository @Inject constructor(
    private val context: Context
) : FlashStateRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val INTENSITY = intPreferencesKey(FlashSettingsMapper.KEY_INTENSITY)
        val STROBE = floatPreferencesKey(FlashSettingsMapper.KEY_STROBE_HZ)
        val UNIT = longPreferencesKey(FlashSettingsMapper.KEY_MORSE_UNIT)
        val ARGB = intPreferencesKey(FlashSettingsMapper.KEY_SCREEN_ARGB)
    }

    private val _isOn = MutableStateFlow(false)
    override val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    override val mode: Flow<FlashMode> = context.dataStore.data.map { p ->
        runCatching { FlashMode.valueOf(p[Keys.MODE] ?: FlashMode.CONTINUOUS.name) }
            .getOrDefault(FlashMode.CONTINUOUS)
    }

    override val settings: Flow<FlashSettings> = context.dataStore.data.map { p ->
        FlashSettingsMapper.fromMap(
            buildMap {
                p[Keys.INTENSITY]?.let { put(FlashSettingsMapper.KEY_INTENSITY, it) }
                p[Keys.STROBE]?.let { put(FlashSettingsMapper.KEY_STROBE_HZ, it) }
                p[Keys.UNIT]?.let { put(FlashSettingsMapper.KEY_MORSE_UNIT, it) }
                p[Keys.ARGB]?.let { put(FlashSettingsMapper.KEY_SCREEN_ARGB, it) }
            }
        )
    }

    override fun setOn(on: Boolean) { _isOn.value = on }

    override suspend fun setMode(mode: FlashMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    override suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        context.dataStore.edit { p ->
            val current = FlashSettingsMapper.fromMap(
                buildMap {
                    p[Keys.INTENSITY]?.let { put(FlashSettingsMapper.KEY_INTENSITY, it) }
                    p[Keys.STROBE]?.let { put(FlashSettingsMapper.KEY_STROBE_HZ, it) }
                    p[Keys.UNIT]?.let { put(FlashSettingsMapper.KEY_MORSE_UNIT, it) }
                    p[Keys.ARGB]?.let { put(FlashSettingsMapper.KEY_SCREEN_ARGB, it) }
                }
            )
            val next = transform(current).coerced()
            p[Keys.INTENSITY] = next.intensityLevel
            p[Keys.STROBE] = next.strobeHz
            p[Keys.UNIT] = next.morseUnitMs
            p[Keys.ARGB] = next.screenArgb
        }
    }
}
```

```kotlin
// FakeFlashStateRepository.kt  (en src/test)
package com.mejoresiagratis.lumiai.util

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFlashStateRepository : FlashStateRepository {
    private val _isOn = MutableStateFlow(false)
    private val _mode = MutableStateFlow(FlashMode.CONTINUOUS)
    private val _settings = MutableStateFlow(FlashSettings())
    override val isOn: StateFlow<Boolean> = _isOn.asStateFlow()
    override val mode = _mode.asStateFlow()
    override val settings = _settings.asStateFlow()
    override fun setOn(on: Boolean) { _isOn.value = on }
    override suspend fun setMode(mode: FlashMode) { _mode.value = mode }
    override suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        _settings.value = transform(_settings.value).coerced()
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/repository app/src/main/kotlin/com/mejoresiagratis/lumiai/data/settings app/src/test/kotlin/com/mejoresiagratis/lumiai/data/settings app/src/test/kotlin/com/mejoresiagratis/lumiai/util/FakeFlashStateRepository.kt
git commit -m "feat(data): DataStore flash state repo + pure settings mapper (TDD)"
```

---

## Task 6: Inyección de dependencias (Hilt)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/di/AppModule.kt`

- [ ] **Step 1: Módulo Hilt**

```kotlin
package com.mejoresiagratis.lumiai.di

import android.content.Context
import com.mejoresiagratis.lumiai.data.settings.DataStoreFlashStateRepository
import com.mejoresiagratis.lumiai.data.torch.Camera2TorchController
import com.mejoresiagratis.lumiai.data.torch.ServiceEngineController
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindTorch(impl: Camera2TorchController): TorchController

    @Binds @Singleton
    abstract fun bindRepo(impl: DataStoreFlashStateRepository): FlashStateRepository

    @Binds @Singleton
    abstract fun bindEngineController(impl: ServiceEngineController): EngineController

    companion object {
        @Provides @Singleton
        fun provideContext(@ApplicationContext ctx: Context): Context = ctx
    }
}
```

> Nota: `Camera2TorchController` y `DataStoreFlashStateRepository` ya reciben `Context` por `@Inject constructor` con el `@ApplicationContext` provisto aquí.

- [ ] **Step 2: Verificar build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt genera componentes).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/di/AppModule.kt
git commit -m "feat(di): Hilt bindings for torch, repo and engine controller"
```

---

## Task 7: FlashViewModel (TDD con MockK + Turbine)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/EngineController.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/FlashUiState.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/FlashViewModel.kt`
- Create: `app/src/test/kotlin/com/mejoresiagratis/lumiai/util/MainDispatcherRule.kt`
- Test: `app/src/test/kotlin/com/mejoresiagratis/lumiai/ui/home/FlashViewModelTest.kt`

- [ ] **Step 1: Interfaz EngineController + UiState**

```kotlin
// EngineController.kt
package com.mejoresiagratis.lumiai.domain.flash

/** Arranca/para el motor (en Fase 1, vía foreground service). */
interface EngineController {
    fun start()
    fun stop()
}
```

```kotlin
// FlashUiState.kt
package com.mejoresiagratis.lumiai.ui.home

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

data class FlashUiState(
    val isOn: Boolean = false,
    val mode: FlashMode = FlashMode.CONTINUOUS,
    val settings: FlashSettings = FlashSettings(),
    val hasFlash: Boolean = true,
    val maxIntensity: Int = 1
)
```

- [ ] **Step 2: Test que falla**

```kotlin
package com.mejoresiagratis.lumiai.ui.home

import app.cash.turbine.test
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.util.FakeFlashStateRepository
import com.mejoresiagratis.lumiai.util.FakeTorchController
import com.mejoresiagratis.lumiai.util.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeFlashStateRepository()
    private val torch: TorchController = FakeTorchController(hasFlash = true, maxIntensityLevel = 100)
    private val engine: EngineController = mockk(relaxed = true)

    private fun vm() = FlashViewModel(repo, engine, torch)

    @Test
    fun `toggle on starts engine and reflects state`() = runTest {
        val vm = vm()
        vm.uiState.test {
            assertEquals(false, awaitItem().isOn)
            vm.toggle()
            assertTrue(awaitItem().isOn)
        }
        verify { engine.start() }
    }

    @Test
    fun `selectMode updates the mode in state`() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.selectMode(FlashMode.STROBE)
            assertEquals(FlashMode.STROBE, awaitItem().mode)
        }
    }

    @Test
    fun `updateSettings clamps out-of-range intensity`() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.updateSettings { it.copy(intensityLevel = 9999) }
            assertEquals(100, awaitItem().settings.intensityLevel)
        }
    }
}
```

- [ ] **Step 3: Ejecutar y ver que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashViewModelTest*"`
Expected: FAIL (`FlashViewModel`/`MainDispatcherRule` no existen).

- [ ] **Step 4: MainDispatcherRule + ViewModel**

```kotlin
// MainDispatcherRule.kt  (en src/test)
package com.mejoresiagratis.lumiai.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```

```kotlin
// FlashViewModel.kt
package com.mejoresiagratis.lumiai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashViewModel @Inject constructor(
    private val repo: FlashStateRepository,
    private val engine: EngineController,
    torch: TorchController
) : ViewModel() {

    val uiState: StateFlow<FlashUiState> =
        combine(repo.isOn, repo.mode, repo.settings) { on, mode, settings ->
            FlashUiState(
                isOn = on,
                mode = mode,
                settings = settings,
                hasFlash = torch.hasFlash,
                maxIntensity = torch.maxIntensityLevel
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlashUiState(hasFlash = torch.hasFlash, maxIntensity = torch.maxIntensityLevel)
        )

    fun toggle() {
        val turningOn = !repo.isOn.value
        repo.setOn(turningOn)
        if (turningOn) engine.start() else engine.stop()
    }

    fun selectMode(mode: FlashMode) {
        viewModelScope.launch { repo.setMode(mode) }
    }

    fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }
}
```

- [ ] **Step 5: Test verde**

Run: `./gradlew :app:testDebugUnitTest --tests "*FlashViewModelTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/flash/EngineController.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home app/src/test/kotlin/com/mejoresiagratis/lumiai/ui/home app/src/test/kotlin/com/mejoresiagratis/lumiai/util/MainDispatcherRule.kt
git commit -m "feat(ui): FlashViewModel with reactive uiState (TDD)"
```

---

## Task 8: Foreground service (conduce el engine con collectLatest)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/TorchService.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/ServiceEngineController.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Servicio**

```kotlin
package com.mejoresiagratis.lumiai.data.torch

import android.app.NotificationChannel
import android.app.NotificationManager
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
            combine(repo.isOn, repo.mode, repo.settings) { on, mode, s -> Triple(on, mode, s) }
                .collectLatest { (on, mode, settings) ->
                    if (!on) {
                        stopSelf()
                    } else {
                        engine.play(mode, settings) // collectLatest cancela esta llamada en cada cambio
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.torch_running))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .build()

    companion object {
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
```

- [ ] **Step 2: EngineController que arranca/para el servicio**

```kotlin
package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceEngineController @Inject constructor(
    private val context: Context
) : EngineController {

    override fun start() {
        TorchService.ensureChannel(context)
        ContextCompat.startForegroundService(context, Intent(context, TorchService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, TorchService::class.java))
    }
}
```

- [ ] **Step 3: Manifest (añadir permiso FGS specialUse + declarar servicio)**

En `app/src/main/AndroidManifest.xml`, añadir junto a los permisos:

```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

Y dentro de `<application>`, antes de `</application>`:

```xml
        <service
            android:name=".data.torch.TorchService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Mantiene la linterna encendida con la app en segundo plano" />
        </service>
```

- [ ] **Step 4: String del servicio**

En `app/src/main/res/values/strings.xml`, añadir:

```xml
    <string name="torch_running">Linterna activa</string>
```

- [ ] **Step 5: Verificar build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/TorchService.kt app/src/main/kotlin/com/mejoresiagratis/lumiai/data/torch/ServiceEngineController.kt app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml
git commit -m "feat(torch): foreground service driving the engine via collectLatest"
```

---

## Task 9: UI Compose (selector + ajustes en vivo + modo Pantalla)

**Files:**
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeSelector.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ModeSettingsPanel.kt`
- Create: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/components/ScreenLight.kt`
- Modify: `app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home/HomeScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: ModeSelector**

```kotlin
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mejoresiagratis.lumiai.domain.model.FlashMode

@Composable
fun ModeSelector(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dpSafe())) {
        FlashMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label()) }
            )
        }
    }
}

private fun FlashMode.label(): String = when (this) {
    FlashMode.CONTINUOUS -> "Continuo"
    FlashMode.SCREEN -> "Pantalla"
    FlashMode.SOS_MORSE -> "SOS"
    FlashMode.STROBE -> "Estrobo"
}

private fun Int.dpSafe() = androidx.compose.ui.unit.dp.let { this.dp }
```

> Nota de implementación: sustituir el helper `dpSafe()` por `import androidx.compose.ui.unit.dp` y usar `8.dp` directamente; se deja el import explícito en el archivo final.

- [ ] **Step 2: ModeSettingsPanel (sliders que aplican al instante)**

```kotlin
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings

@Composable
fun ModeSettingsPanel(
    mode: FlashMode,
    settings: FlashSettings,
    maxIntensity: Int,
    onChange: ((FlashSettings) -> FlashSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (maxIntensity > 1 && mode != FlashMode.SCREEN) {
            Text("Intensidad: ${settings.intensityLevel}%")
            Slider(
                value = settings.intensityLevel.toFloat(),
                onValueChange = { v -> onChange { it.copy(intensityLevel = v.toInt()) } },
                valueRange = FlashSettings.MIN_INTENSITY.toFloat()..FlashSettings.MAX_INTENSITY.toFloat()
            )
        }
        when (mode) {
            FlashMode.STROBE -> {
                Text("Frecuencia: ${"%.1f".format(settings.strobeHz)} Hz")
                Slider(
                    value = settings.strobeHz,
                    onValueChange = { v -> onChange { it.copy(strobeHz = v) } },
                    valueRange = FlashSettings.MIN_STROBE_HZ..FlashSettings.MAX_STROBE_HZ
                )
            }
            FlashMode.SOS_MORSE -> {
                Text("Velocidad (unidad): ${settings.morseUnitMs} ms")
                Slider(
                    value = settings.morseUnitMs.toFloat(),
                    onValueChange = { v -> onChange { it.copy(morseUnitMs = v.toLong()) } },
                    valueRange = FlashSettings.MIN_UNIT_MS.toFloat()..FlashSettings.MAX_UNIT_MS.toFloat()
                )
            }
            else -> { /* CONTINUOUS / SCREEN: sin parámetros extra en Fase 1 */ }
        }
    }
}
```

- [ ] **Step 3: ScreenLight (overlay blanco para modo Pantalla)**

```kotlin
package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ScreenLight(argb: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(argb)))
}
```

- [ ] **Step 4: HomeScreen (reescribir el placeholder de Fase 0)**

```kotlin
package com.mejoresiagratis.lumiai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.home.components.ModeSelector
import com.mejoresiagratis.lumiai.ui.home.components.ModeSettingsPanel
import com.mejoresiagratis.lumiai.ui.home.components.ScreenLight

@Composable
fun HomeScreen(viewModel: FlashViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isOn && state.mode == FlashMode.SCREEN) {
        ScreenLight(argb = state.settings.screenArgb)
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeSelector(selected = state.mode, onSelect = viewModel::selectMode)
            ModeSettingsPanel(
                mode = state.mode,
                settings = state.settings,
                maxIntensity = state.maxIntensity,
                onChange = viewModel::updateSettings
            )
            Button(onClick = viewModel::toggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isOn) "Apagar" else "Encender")
            }
            if (!state.hasFlash && state.mode != FlashMode.SCREEN) {
                Text("Este dispositivo no tiene flash: usa el modo Pantalla.")
            }
        }
    }
}
```

- [ ] **Step 5: Verificar build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/ui/home
git commit -m "feat(ui): home screen with mode selector, live settings and screen mode"
```

---

## Task 10: Integración final, permiso de notificaciones y verificación

**Files:**
- Modify: `app/src/main/kotlin/com/mejoresiagratis/lumiai/MainActivity.kt` (pedir POST_NOTIFICATIONS en Android 13+)

- [ ] **Step 1: Pedir POST_NOTIFICATIONS (Android 13+) en MainActivity**

```kotlin
// añadir en MainActivity.onCreate, tras enableEdgeToEdge():
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

// ...
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }
        .launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

> Implementación final: declarar el launcher como propiedad de la Activity y lanzarlo en `onCreate` (no inline), para cumplir el contrato de `registerForActivityResult`.

- [ ] **Step 2: Suite completa de tests verde**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (Morse, FlashEngine, FlashSettings, mapper, ViewModel).

- [ ] **Step 3: Build debug**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Checklist de verificación manual on-device**

- [ ] Encender → Continuo enciende el LED; apagar lo apaga.
- [ ] Cambiar Continuo → Estrobo **sin parpadeo residual** ni LED “pegado”.
- [ ] Mover el slider de frecuencia con el estrobo activo → cambia **al instante**.
- [ ] SOS reproduce ··· --- ··· en bucle; mover “unidad” cambia la velocidad en vivo.
- [ ] Modo Pantalla muestra pantalla blanca a brillo alto; el LED queda apagado.
- [ ] Mover el slider de intensidad (dispositivo con control de brillo) cambia en vivo.
- [ ] Mandar la app a segundo plano con la linterna activa → sigue encendida (servicio + notificación).
- [ ] Apagar desde la app → LED off y notificación desaparece.
- [ ] Reabrir la app → recupera último modo y ajustes (DataStore).

- [ ] **Step 5: Commit + push**

```bash
git add app/src/main/kotlin/com/mejoresiagratis/lumiai/MainActivity.kt
git commit -m "feat(app): request notification permission; finalize Fase 1"
git push origin main
```

- [ ] **Step 6: Verificar CI verde** (polling REST por `head_sha`, paso `Build debug APK`).

---

## Self-Review (hecho)

- **Cobertura del spec:** 4 modos (T1/T4/T9), LED único (T3), ajustes en vivo + sin conflictos (`collectLatest` en T8 + `FlashEngine` cancelable en T4), persistencia (T5), segundo plano (T8). ✔
- **Sin placeholders:** todo paso con código lleva código completo; las dos `Nota`s marcan refinamientos triviales de import/launcher, no lógica pendiente. ✔
- **Consistencia de tipos:** `FlashMode`, `FlashSettings`, `TorchController`, `FlashEngine`, `FlashStateRepository`, `EngineController`, `FlashViewModel`, `FlashUiState` se usan con la misma firma en todas las tareas. ✔
- **Riesgos conocidos:** (1) FGS tipo `specialUse` → vigilar criterio de revisión de Play; (2) intensidad solo en API 33+ con `maxLevel>1`; (3) tests instrumentados de UI/servicio quedan para P2.
