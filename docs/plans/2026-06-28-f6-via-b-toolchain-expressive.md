# Fase 6 · Vía B — Toolchain bump + M3 Expressive real (RAMA)

> Rama: `restyle/f6-via-b-m3-expressive` (NO tocar `main`). `main` se queda con la Vía A (F0–F5).
> Restaurar/abandonar: `git checkout main` (la rama queda como referencia). Backup adicional:
> `backup/pre-m3-restyle` + tag `backup-pre-m3-restyle-20260628`.

Esta fase **no se puede validar en el sandbox** (sin Gradle/SDK). Es un andamiaje para que
**tú compiles y pruebes**; el primer build seguramente pedirá reconciliar versiones. Mándame el
log y lo iteramos. Las versiones son objetivos verificados en la web (jun 2026) pero hay que
confirmarlas contra lo que resuelva Gradle en tu máquina.

## 1. Qué he cambiado en esta rama

**`gradle/libs.versions.toml`**
- `agp` 8.7.3 → **8.12.0** (necesario para `compileSdk 36` y Compose 1.11). Mantener AGP **8.x**
  a propósito: AGP 9.0 trae "Kotlin integrado" y obliga a retirar el plugin `kotlin-android`
  (migración aparte, más riesgo). No saltar a 9.x en esta fase.
- `kotlin` 2.0.21 → **2.2.10**. El compose-compiler viaja con Kotlin (plugin `kotlin-compose`),
  así que no hay `kotlinCompilerExtensionVersion` que tocar.
- `ksp` → **2.2.10-2.0.2** (debe coincidir EXACTO con Kotlin; si no, KSP falla en seco).
- `hilt` 2.52 → **2.57.1** (compatibilidad con Kotlin 2.2 / KSP nuevo). **Verificar** la última.
- `composeBom` 2024.09.03 → **2026.06.00**.
- Nuevo `material3Expressive = "1.5.0-alpha22"` + librería `androidx-compose-material3-expressive`.
  Las APIs Expressive reales viven en **material3 1.5.0-alpha** (estable es 1.4.0, baseline M3).
  **Verificar** la última alpha al compilar (puede haber salido 1.5.0-alpha23+).

**`app/build.gradle.kts`**
- `compileSdk` 35 → **36** (Android 16). `targetSdk` se queda en **35** (evitar cambios de
  comportamiento de Android 16 por ahora; subir a 36 es decisión aparte, trae edge-to-edge, etc.).
- `material3` → override a `libs.androidx.compose.material3.expressive` (1.5.0-alpha).
- El bloque `-Xskip-metadata-version-check`: comentario actualizado. Con Kotlin 2.2.10 ya se lee
  AdMob 25.x; mantengo el flag por si la alpha de material3 viene con metadata de un Kotlin más
  nuevo. **Si el build pasa sin él, retíralo.**

**`ui/theme/Theme.kt`** (el cambio central, ya hecho)
- `MaterialTheme(...)` → **`MaterialExpressiveTheme(...)`** con `motionScheme = MotionScheme.expressive()`
  y `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Conserva intactos `colorScheme`
  (materialKolor/dynamic), `LumiShapes` y `LumiAiTypography`. Esto aplica los **muelles oficiales
  de M3 Expressive a TODOS los componentes** de golpe (botones, segmented, switches, sliders…).

## 2. Reconciliación esperada en el PRIMER compile (mándame el log)

Cosas que casi seguro habrá que ajustar (no las toco a ciegas):
- **Hilt**: versión exacta compatible con Kotlin 2.2.10 / KSP 2.2.10-2.0.2.
- **Robolectric** 4.14.1: puede necesitar bump para API 36 / Kotlin 2.2 (p. ej. 4.15.x).
- **androidx core/lifecycle/navigation/activity-compose**: con compileSdk 36 + Compose 1.11
  Gradle puede pedir subirlas (coreKtx 1.16, lifecycle 2.9, navigation 2.9, activityCompose 1.10).
- **AGP ↔ compileSdk 36**: si AGP 8.12 se queja, o bien subir AGP, o `android.suppressUnsupportedCompileSdk=36`.
- **Tests v2 de Compose** (BOM 2026.04+): el dispatcher de test pasó a `StandardTestDispatcher`.
  Nuestros tests de semántica ya usan `autoAdvance = false`, pero revisar `PowerOrbSemanticsTest`
  y `ModePillSemanticsTest` por si cambia el timing.

## 3. Checklist de adopción Expressive (iterativo, con compile feedback)

Una vez compile el tema Expressive, sustituir nuestros "muelles/formas a mano" (Vía A) por las
APIs reales, una a una, validando en dispositivo. Todo bajo `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`:

- [ ] **Orbe (Beam Hub)**: cambiar el `animateDpAsState` del corner por **`MaterialShapes`** +
  morph de forma oficial (p. ej. cookie/pill) si encaja con el diseño honesto.
- [ ] **Rail de modos**: evaluar **`ButtonGroup`** / **`FloatingToolbar`** Expressive en lugar de
  los `ModePill` manuales (mantener semántica `Role.Tab`/locked).
- [ ] **Sliders** (intensidad, baliza, estrobo, morse, brillo): **slider Expressive con surco
  ondulado (wavy)** donde aporte; mantener rangos/semántica.
- [ ] **Progreso**: si hay spinners, **`LoadingIndicator`/`CircularWavyProgressIndicator`**.
- [ ] **Botones/segmented**: ya heredan `MotionScheme`; revisar si conviene `tonalColors()` y las
  nuevas formas de TextField Expressive en Auth.
- [ ] Revisar que `LumiMotion` (Vía A) y `MotionScheme` no se pisen; probablemente `LumiMotion`
  se puede **retirar** una vez todo use el motion scheme del tema (preguntar antes de borrar).

## 4. Cómo probar
1. `./gradlew :app:assembleDebug` (o desde Android Studio). Mándame el primer log de error.
2. Iteramos versiones hasta que compile.
3. `./gradlew :app:testDebugUnitTest` para los tests de semántica.
4. QA visual en dispositivo (claro/oscuro, ámbar) comparando con los mockups de Vía A.

## 5. Estado
- [x] Toolchain bump (catálogo + build.gradle) — **sin verificar (requiere tu compile)**
- [x] `Theme.kt` → `MaterialExpressiveTheme` + `MotionScheme`
- [ ] Reconciliación de versiones (con tu log)
- [ ] Adopción Expressive componente a componente
- [ ] QA en dispositivo

---

## RESULTADO REAL DEL BUILD (28 jun 2026, verificado compilando)

Se compiló de verdad en un entorno con JDK 17 + Android SDK 36 (Gradle 8.13). Hallazgos:

**A1 · Baseline modernizado — VERDE y verificado** ✅
- `assembleDebug` → **BUILD SUCCESSFUL** (APK debug generado, 45.7 MB).
- `testDebugUnitTest` → **BUILD SUCCESSFUL** (todos los tests pasan, incluidos los de semántica
  `PowerOrb`/`ModePill` con Robolectric; el cambio a test v2 de Compose del BOM no los rompió).
- Set verificado: **AGP 8.12.0, Gradle 8.13, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Hilt 2.57.1,
  Compose BOM 2026.06.00 (→ material3 1.4.0 estable), compileSdk 36, targetSdk 35, JDK 17.**
- El bloque `-Xskip-metadata-version-check` se **RETIRÓ**: con Kotlin 2.2.10 ya no hace falta
  (era por AdMob 25.x). Recompilado sin él → sigue verde.
- ⚠️ El wrapper de Gradle se subió a **8.13** (AGP 8.12 lo exige; antes 8.9).
- Todo el restyle de Vía A (F0–F5) sigue intacto y compila sobre este toolchain.

**A2 · Expressive real (material3 1.5.0-alpha) — BLOQUEADO por toolchain** ⛔ (gated)
- El override a `material3:1.5.0-alpha22` arrastra `compose-ui:1.12.0-alpha03`, que **exige
  AGP 9.1.0+ y compileSdk 37 (Android 17)**. Mensaje literal del build:
  *"requires Android Gradle plugin 9.1.0 or higher"* y *"compile against version 37 or later"*.
- Además `platforms;android-37` no está disponible con cmdline-tools de 2024 (repo ya en XML v4).
- Conclusión: **Vía B real = migrar a AGP 9.1 (Kotlin integrado) + compileSdk 37 + Compose alpha**.
  Es una decisión de proyecto (compilar contra Android 17 preview y Compose alpha), no un simple
  bump. Hacerlo cuando material3 1.5 se acerque a estable o haya razón de negocio.

**Cómo pasar de A1 a A2 cuando toque** (flip documentado):
1. Subir AGP a 9.1.0+ (catálogo) y el wrapper a la Gradle que pida.
2. Migración AGP 9 "Kotlin integrado": o `android.builtInKotlin=false` (mantener `kotlin-android`)
   o retirar el plugin `kotlin-android`. Migrar `kotlinOptions{}` → `kotlin{ compilerOptions{} }`.
3. `compileSdk = 37` (instalar `platforms;android-37` con cmdline-tools recientes).
4. `implementation(libs.androidx.compose.material3.expressive)` (ya está la entrada en el catálogo).
5. `Theme.kt`: `MaterialTheme` → `MaterialExpressiveTheme` + `MotionScheme.expressive()` (@OptIn).
6. Iterar el checklist de componentes Expressive (sección 3).

> Estado de versiones para A2 en el catálogo: la entrada `material3Expressive = "1.5.0-alpha22"`
> y `androidx-compose-material3-expressive` quedan presentes pero **sin usar** en A1 (listas para el flip).

### Estado (actualizado)
- [x] A1 baseline modernizado — **compila + tests verdes (verificado)**
- [x] `-Xskip-metadata-version-check` retirado (verificado)
- [ ] A2 Expressive real — gated tras AGP 9.1 + compileSdk 37 (Android 17)
