# Modernización de toolchain (A1) — verificado compilando

PR enfocado: **solo** sube el toolchain a versiones actuales (jun 2026), sin tocar UI ni lógica.
Todo el restyle M3 (Vía A, F0–F5) ya en `main` sigue intacto y compila sobre este stack.

## Cambios
| Componente | Antes | Después |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | **8.12.0** |
| Gradle wrapper | 8.9 | **8.13** (lo exige AGP 8.12) |
| Kotlin | 2.0.21 | **2.2.10** |
| KSP | 2.0.21-1.0.28 | **2.2.10-2.0.2** |
| Hilt | 2.52 | **2.57.1** |
| Compose BOM | 2024.09.03 | **2026.06.00** (→ material3 1.4.0) |
| compileSdk | 35 | **36** |

- Se **retira** el flag `-Xskip-metadata-version-check`: con Kotlin 2.2.10 ya no hace falta
  (era por la metadata de AdMob 25.x). Recompilado sin él → verde.
- `targetSdk` se mantiene en 35 (decisión de release aparte).

## Verificación (compilado de verdad, JDK 17 + Android SDK 36)
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (APK debug 45.7 MB).
- `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (todos los tests, incl. semántica
  `PowerOrb`/`ModePill` con Robolectric; el cambio a los test v2 de Compose del BOM no los rompió).

## Fuera de alcance (gated)
M3 Expressive **real** (`MaterialExpressiveTheme`, `MotionScheme`, `MaterialShapes`, wavy…) vive en
material3 1.5.0-alpha y **exige AGP 9.1 + compileSdk 37 (Android 17)** + migración a "Kotlin integrado"
de AGP 9. Queda para más adelante; pasos del flip documentados en la rama
`restyle/f6-via-b-m3-expressive` (`docs/plans/2026-06-28-f6-via-b-toolchain-expressive.md`).
