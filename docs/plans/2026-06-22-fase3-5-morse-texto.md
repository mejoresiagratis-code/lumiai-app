# Fase 3.5 — Primer modo avanzado: "Morse de texto" + gating visible

> Primer modo de pago real. Activa de verdad el framework de entitlements de la Fase 3: el modo aparece con candado para invitados y se desbloquea al crear cuenta.

**Goal:** Un modo que emite en Morse cualquier texto que escriba el usuario (repitiéndolo), reutilizando el encoder `Morse.toDurations` ya existente y el timing `morseUnitMs`. Es **ADVANCED**: bloqueado para invitados (candado → lleva a *Acceder*), desbloqueado con cuenta. Honesto: sin "IA", solo LED + temporización estándar.

**Decisiones de UX a confirmar:**
- Modo bloqueado: la tarjeta se muestra con **candado**; al tocarla **no se selecciona**, sino que navega a *Acceder* (crear cuenta desbloquea). 
- Si quedara persistido un modo avanzado y el usuario cierra sesión, al volver a Home se **resetea a Continuo** (no se reproduce gratis).

**Architecture:** se añade `FlashMode.TEXT_MORSE`; `tier = ADVANCED`. `FlashSettings` gana `morseText`. El motor añade una rama `textMorse` (mismo patrón que `morse`/SOS, pero con el texto del usuario y `collectLatest` para aplicar cambios en vivo). El `FlashViewModel` expone `entitlements` (de `EntitlementRepository`, ya creado en Fase 3) en `FlashUiState`; el grid pinta candado y enruta a *Acceder*. Sin tocar el `TorchController` ni el servicio.

**Tech Stack:** Kotlin/Compose/Hilt/DataStore (sin dependencias nuevas).

---

## File Structure

```
domain/model/FlashMode.kt              # (mod) + TEXT_MORSE
domain/model/FlashSettings.kt          # (mod) + morseText + MAX_MORSE_LEN + coerced()
domain/entitlement/Entitlements.kt     # (mod) FlashMode.tier: TEXT_MORSE -> ADVANCED
domain/flash/ModeControls.kt           # (mod) controls()/requiresFlash() para TEXT_MORSE
domain/flash/FlashEngine.kt            # (mod) rama TEXT_MORSE -> textMorse()
data/settings/FlashSettingsMapper.kt   # (mod) KEY_MORSE_TEXT
data/settings/DataStoreFlashStateRepository.kt # (mod) persistir morseText
ui/home/FlashUiState.kt                # (mod) + entitlements
ui/home/FlashViewModel.kt              # (mod) inyectar EntitlementRepository, combine 4
ui/home/components/ModeUi.kt           # (mod) catálogo + TEXT_MORSE (isPro)
ui/home/components/ModeCard.kt         # (mod) estado locked + candado
ui/home/components/ModeGrid.kt         # (mod) entitlements -> locked, onLocked
ui/home/components/ModeSettingsPanel.kt# (mod) campo de texto cuando TEXT_MORSE
ui/home/HomeScreen.kt                  # (mod) onOpenAuth, reset si locked
ui/navigation/LumiAiNavHost.kt         # (mod) pasar onOpenAuth a HomeScreen
res/drawable/ic_mode_morse_text.xml    # (nuevo) icono
res/drawable/ic_lock.xml               # (nuevo) candado
res/values/strings.xml                 # (mod) textos
app/src/test/.../domain/flash/MorseTest.kt        # (nuevo) tests encoder
app/src/test/.../domain/entitlement/EntitlementsTest.kt # (mod) TEXT_MORSE = ADVANCED
```

---

## Task 1: Encoder Morse — tests (el código ya existe)

`Morse.toDurations` ya está implementado. Solo blindamos su comportamiento con tests.

- [ ] `app/src/test/.../domain/flash/MorseTest.kt`:
```kotlin
package com.mejoresiagratis.lumiai.domain.flash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTest {
    @Test fun `E es un punto (1u ON)`() {
        assertArrayEquals(longArrayOf(100), Morse.toDurations("E", 100))
    }
    @Test fun `T es una raya (3u ON)`() {
        assertArrayEquals(longArrayOf(300), Morse.toDurations("T", 100))
    }
    @Test fun `EE separa caracteres con 3u OFF`() {
        // E(1) OFF(3) E(1)
        assertArrayEquals(longArrayOf(100, 300, 100), Morse.toDurations("EE", 100))
    }
    @Test fun `espacio entre palabras es 7u`() {
        // "E E" -> E(1) WORD(7) E(1)
        assertArrayEquals(longArrayOf(100, 700, 100), Morse.toDurations("E E", 100))
    }
    @Test fun `ignora caracteres no soportados`() {
        assertArrayEquals(Morse.toDurations("E", 100), Morse.toDurations("E@#", 100))
    }
    @Test fun `texto vacio o sin codigo da array vacio`() {
        assertEquals(0, Morse.toDurations("", 100).size)
        assertEquals(0, Morse.toDurations("@@@", 100).size)
    }
    @Test fun `sos sigue funcionando`() {
        assertTrue(Morse.sos(100).isNotEmpty())
    }
}
```

---

## Task 2: Dominio — modo, ajuste y tier

- [ ] **FlashMode**: `enum class FlashMode { CONTINUOUS, SCREEN, SOS_MORSE, STROBE, TEXT_MORSE }`

- [ ] **FlashSettings**: añadir campo + límite + coerción:
```kotlin
data class FlashSettings(
    val intensityLevel: Int = MAX_INTENSITY,
    val strobeHz: Float = 8f,
    val morseUnitMs: Long = 200L,
    val screenArgb: Int = -0x1,
    val morseText: String = "SOS"
) {
    fun coerced() = copy(
        intensityLevel = intensityLevel.coerceIn(MIN_INTENSITY, MAX_INTENSITY),
        strobeHz = strobeHz.coerceIn(MIN_STROBE_HZ, MAX_STROBE_HZ),
        morseUnitMs = morseUnitMs.coerceIn(MIN_UNIT_MS, MAX_UNIT_MS),
        morseText = morseText.take(MAX_MORSE_LEN)
    )
    companion object {
        // ... consts existentes ...
        const val MAX_MORSE_LEN = 50
    }
}
```
> Se permite texto en blanco (el motor no emite nada hasta que haya algo válido); no se fuerza a "SOS" mientras se escribe.

- [ ] **Entitlements** (`FlashMode.tier`): añadir `FlashMode.TEXT_MORSE -> Tier.ADVANCED` (el resto sigue BASIC). Test en `EntitlementsTest`:
```kotlin
@Test fun `text morse es advanced`() {
    assertEquals(Tier.ADVANCED, com.mejoresiagratis.lumiai.domain.model.FlashMode.TEXT_MORSE.tier)
}
```

- [ ] **ModeControls**: `requiresFlash()` (TEXT_MORSE requiere flash → ya cubierto porque `!= SCREEN`). `controls()`:
```kotlin
FlashMode.TEXT_MORSE -> setOf(ModeControl.INTENSITY, ModeControl.MORSE_SPEED)
```

---

## Task 3: Motor + persistencia

- [ ] **FlashEngine.play** — añadir rama y función:
```kotlin
FlashMode.TEXT_MORSE -> settings.collectLatest { textMorse(it.coerced()) }
```
```kotlin
private suspend fun textMorse(s: FlashSettings) {
    val durations = Morse.toDurations(s.morseText, s.morseUnitMs)
    if (durations.isEmpty()) { torch.turnOff(); return } // nada válido que emitir
    while (true) {
        for (i in durations.indices) {
            if (i % 2 == 0) torch.turnOn(s.intensityLevel) else torch.turnOff()
            delay(durations[i])
        }
        torch.turnOff()
        delay(s.morseUnitMs * 7) // separación entre repeticiones
    }
}
```

- [ ] **FlashSettingsMapper**: `KEY_MORSE_TEXT = "morse_text"`; en `toMap` añadir `KEY_MORSE_TEXT to s.morseText`; en `fromMap` `morseText = (m[KEY_MORSE_TEXT] as? String) ?: d.morseText`.

- [ ] **DataStoreFlashStateRepository**: `Keys.TEXT = stringPreferencesKey(FlashSettingsMapper.KEY_MORSE_TEXT)`; escribir `p[Keys.TEXT] = next.morseText` en `updateSettings`; leer en `readMap`.

---

## Task 4: UI — catálogo, candado y campo de texto

- [ ] **ModeUi (catálogo)**: añadir
```kotlin
ModeUi(FlashMode.TEXT_MORSE, R.string.mode_text_morse, R.drawable.ic_mode_morse_text, isPro = true)
```

- [ ] **FlashUiState**: `val entitlements: Entitlements = Entitlements()`.

- [ ] **FlashViewModel**: inyectar `entitlementRepo: EntitlementRepository`; `combine(repo.isOn, repo.mode, repo.settings, entitlementRepo.entitlements) { on, mode, settings, ent -> FlashUiState(on, mode, settings, capabilities, ent) }`.

- [ ] **ModeCard**: parámetro `locked: Boolean = false`; si locked, superponer un pequeño candado (`ic_lock`) en la esquina y atenuar levemente; `onClick` se mantiene (decide el grid).

- [ ] **ModeGrid**: recibir `entitlements: Entitlements`, `onLocked: (FlashMode) -> Unit`; para cada item `val locked = !entitlements.unlocks(item.mode.tier)`; `onClick = { if (locked) onLocked(item.mode) else onSelect(item.mode) }`; pasar `locked` a `ModeCard`.

- [ ] **ModeSettingsPanel**: cuando `mode == FlashMode.TEXT_MORSE`, mostrar un `OutlinedTextField` (label `settings_morse_text`, `singleLine`, `maxLength` visual) encima de los sliders:
```kotlin
if (mode == FlashMode.TEXT_MORSE) {
    OutlinedTextField(
        value = settings.morseText,
        onValueChange = { v -> onChange { it.copy(morseText = v.take(FlashSettings.MAX_MORSE_LEN)) } },
        label = { Text(stringResource(R.string.settings_morse_text)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
```

- [ ] **HomeScreen**: nuevo parámetro `onOpenAuth: () -> Unit`. Pasar a `ModeGrid(entitlements = state.entitlements, onSelect = viewModel::selectMode, onLocked = { onOpenAuth() }, ...)`. Reset de seguridad:
```kotlin
LaunchedEffect(state.entitlements, state.mode) {
    if (!state.entitlements.unlocks(state.mode.tier)) viewModel.selectMode(FlashMode.CONTINUOUS)
}
```

- [ ] **LumiAiNavHost**: `HomeScreen(onOpenSettings = …, onOpenAuth = { navController.navigate(Routes.AUTH) })`.

- [ ] **Drawables**: `ic_mode_morse_text.xml` (punto-raya) y `ic_lock.xml` (candado simple), vectores monocromo `?attr/colorControlNormal`-friendly (tint heredado).

- [ ] **Strings (ES)**: `mode_text_morse` = "Morse de texto"; `settings_morse_text` = "Mensaje"; `mode_pro_badge` = "PRO" (si se usa en la tarjeta).

---

## Task 5: Verificación

- [ ] `:app:testDebugUnitTest` PASS (MorseTest + EntitlementsTest).
- [ ] `:app:assembleDebug` BUILD SUCCESSFUL.
- [ ] Push + CI verde por `head_sha`.
- [ ] On-device:
  - [ ] Como **invitado**: "Morse de texto" aparece con **candado**; al tocar → pantalla *Acceder*.
  - [ ] **Con cuenta**: se selecciona; escribo un texto, **Encender** → la linterna lo emite en Morse y se repite.
  - [ ] Cambiar el texto o la velocidad se aplica **en vivo** sin reiniciar.
  - [ ] Cerrar sesión con TEXT_MORSE seleccionado → vuelve a **Continuo**.

---

## Self-Review (hecho)

- **Honesto:** Morse estándar real, sin "IA"; reutiliza encoder existente. ✔
- **Gating visible:** primer modo que ejercita `Entitlements`; candado + ruta a *Acceder*. ✔
- **Patrón del motor respetado:** on/off y cambio de modo relanzan; ajustes en vivo por Flow (`collectLatest`). ✔
- **Sin tocar hardware:** ni `TorchController` ni `TorchService` cambian. ✔
- **Riesgo:** texto en blanco = no emite (intencionado). El reset al cerrar sesión evita uso gratis del avanzado. La compatibilidad de DataStore es aditiva (clave nueva, default "SOS").
