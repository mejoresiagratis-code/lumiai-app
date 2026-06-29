# A2 · Android 17 (API 37) + M3 Expressive real — VERIFICADO compilando

Rama `restyle/android17-expressive` (sobre la base modernizada A1). Sube a Android 17 y activa
las APIs **reales** de Material 3 Expressive. **Bleeding-edge**: ver caveats al final.

## Verificación (compilado de verdad, JDK17 + Android SDK con API 37)
- `:app:assembleDebug` → **BUILD SUCCESSFUL** (APK debug).
- `:app:testDebugUnitTest` → **BUILD SUCCESSFUL** (toda la semántica `PowerOrb`/`ModePill` con
  Robolectric pasa sobre el toolchain nuevo).

## Toolchain
| Componente | A1 | A2 (Android 17) |
|---|---|---|
| AGP | 8.12.0 | **9.2.0** (soporta API 37) |
| Gradle | 8.13 | **9.6.1** |
| Kotlin | 2.2.10 | **2.3.10** (lo exige el plugin kotlin-android con AGP 9) |
| KSP | 2.2.10-2.0.2 | **2.3.5** (KSP ya es independiente de Kotlin) |
| Hilt | 2.57.1 | **2.59.2** (lee metadata de Kotlin 2.3) |
| compileSdk | 36 | **37 (Android 17)** |
| material3 | 1.4.0 (BOM) | **1.5.0-alpha22** (Expressive, experimental) |
| Theme | MaterialTheme | **MaterialExpressiveTheme + MotionScheme.expressive()** |

## Cadena de arreglos de la migración a AGP 9 (orden real en que aparecieron)
1. **"Cannot add extension with name 'kotlin'"** → AGP 9 activa *built-in Kotlin* y choca con
   `kotlin-android`. Solución elegida (menor churn, conserva compose plugin + KSP + Hilt):
   `android.builtInKotlin=false` en `gradle.properties`.
2. **`ApplicationExtensionImpl cannot be cast to BaseExtension`** → `kotlin-android` no soporta el
   *new DSL* de AGP 9 (activo por defecto). Bypass soportado hasta AGP 10:
   `android.newDsl=false`. (Y `kotlin-android` debe ser 2.3.x para AGP 9 → Kotlin 2.3.10.)
3. **`kotlinOptions` retirado en KGP 2.3** → migrado a `kotlin { compilerOptions {
   jvmTarget.set(JvmTarget.JVM_17) } }`.
4. **Hilt metadata 2.3.0 > max 2.2.0** → Hilt 2.57.1 no leía metadata de Kotlin 2.3 → **Hilt 2.59.2**.

## Instalación del SDK de Android 17 (nota de reproducibilidad)
El paquete usa el esquema nuevo: **`platforms;android-37.0`** y `build-tools;37.0.0` (no `android-37`).
`compileSdk = 37` resuelve contra esa plataforma.

## ⚠️ Caveats (honestidad)
- **M3 Expressive es alpha/experimental.** material3 1.5.0 es alpha (hoy `-alpha22`); sus APIs van
  con `@ExperimentalMaterial3ExpressiveApi` y **cambiarán**. Google promociona 1.5.0 a estable
  "más adelante en 2026".
- **`newDsl=false` es un parche temporal** (se elimina en AGP 10, mediados de 2026). El camino
  definitivo es migrar a *built-in Kotlin* (quitar `kotlin-android`, `builtInKotlin=true`).
- **`targetSdk` sigue en 35.** Android 17 no es final; no se puede publicar en Play con targetSdk 37
  todavía. Esto compila *contra* Android 17, no publica para Android 17.
- **No mergear a `main` aún.** `main` debe quedarse en algo publicable (A1 estable). Esta rama es
  para construir/iterar el look Expressive hasta que 1.5 sea estable y Android 17 final.
- Falta **QA en dispositivo** (Pablo).

## Cuando 1.5 sea estable + Android 17 final
- Bajar el override a la material3 estable que toque (quitar el `-alpha`).
- Subir `targetSdk` a 37 y revisar comportamientos nuevos de Android 17.
- Migrar a built-in Kotlin y retirar `newDsl=false`/`builtInKotlin=false`.
