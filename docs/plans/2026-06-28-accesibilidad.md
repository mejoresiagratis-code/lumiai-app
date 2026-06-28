# Hito · Accesibilidad (Fase 1.5) — base para Google Play

> **Estado (28 jun): COMPLETO.** Lotes 1–5 implementados y **Lote 6 (QA en dispositivo) hecho**
> (Accessibility Scanner + TalkBack + fuente 200% + contraste por acento). Único resto diferido:
> tests instrumentados de `semantics` (no bloquean publicación).

Fecha: 28 jun 2026 · HEAD base: `bc93765`
Objetivo: cerrar la accesibilidad de la **Fase 1.5** del roadmap de producto y cubrir el
requisito real de calidad de Google Play. La mayoría es **comportamiento automático** (no
opción); solo un puñado de cosas son preferencia real y van a una **sección "Accesibilidad"**
en Ajustes, junto a "Apariencia".

## Principio rector
- **Lo básico de accesibilidad NO se gatea** detrás de un toggle: se implementa y respeta el
  sistema (tamaño de fuente, reducir movimiento, TalkBack). Gatearlo es antipatrón.
- **La hoja "Control" (por modo) NO lleva accesibilidad**: es contextual. Las preferencias van
  a **Ajustes → Accesibilidad** (descubrible, donde lo busca la review de Play).
- Honestidad: nada de esto debe falsear capacidades del hardware; se gatea por
  `DeviceCapabilities` donde aplique (p. ej. háptica solo si hay vibrador).

## Estado actual (real, leído del repo)
- `contentDescription` **parcial**: existen `settings_cd`, `mode_locked_cd`, `info_cd`,
  `back` en `AuthScreen`/`SettingsScreen`. El **orbe** (PowerOrb), las **pills** de modo, los
  **sliders** y el **asa** de Pantalla no anuncian estado/rol de forma robusta.
- Tipografía en `sp` (Compose) → escala dinámica funciona por defecto, **sin verificar** a
  200% (riesgo de recortes por `maxLines`/`Ellipsis` en pills y etiquetas).
- **Sin** soporte explícito de "reducir movimiento": el pulso del orbe, los ticks y la
  animación de color corren siempre.
- **Sin** háptica. `DeviceCapabilities(hasFlash, maxTorchLevel)` no expone vibrador.
- Persistencia en DataStore: `DataStoreThemePreferencesRepository` (tema/acento),
  `...OnboardingPreferencesRepository`, `...FlashStateRepository`. **No hay** prefs de a11y.
- Tema generado en `Theme.kt` con `rememberDynamicColorScheme(...)` de MaterialKolor — admite
  `contrastLevel`, hoy no se usa (queda en 0.0). El tinte fuerte de neutros con
  `PaletteStyle.Content` puede **bajar el contraste** en algunos acentos: hay que medirlo.
- `SettingsScreen` ya tiene sección **Apariencia** (tema + acento) y `ThemeViewModel`.

---

## Capa A — Automático (sin opción). Esto es el grueso del requisito de Play
Verificación: **Accessibility Scanner**, **TalkBack** manual, tests de `semantics` en Compose
y el **pre-launch report** de Play. No requiere build local para diseñarlo, pero **sí** prueba
en dispositivo antes de publicar.

1. **Semántica / TalkBack** en todo control:
   - PowerOrb: `Modifier.semantics { role = Button; stateDescription = "Encendida/Apagada"; contentDescription = "Linterna" }` + **live region** (`liveRegion = Polite`) para anunciar el cambio al encender/apagar.
   - Pills de modo: rol `Tab`/`Button`, `selected`, `contentDescription` = nombre del modo; estado "bloqueado" anunciado.
   - Sliders (intensidad, intervalo, destello, tono, brillo): `contentDescription` + valor; ya son `Slider` (accesible), añadir etiqueta y `stateDescription` con el valor formateado.
   - Icono ⓘ, asa de Pantalla, swatches/preset chips: `contentDescription` con rol de botón.
2. **Objetivos táctiles ≥ 48dp**: el dibujo puede ser pequeño, pero el área de toque ≥48dp.
   Revisar **icono ⓘ (20dp)**, **asa de Pantalla**, **swatches (34dp)** → envolver en
   `Modifier.sizeIn(minWidth=48dp, minHeight=48dp)` o `minimumInteractiveComponentSize()`.
3. **Contraste** ≥ 4.5:1 texto / ≥ 3:1 UI. Medir con el tinte `Content`; si algún acento no
   pasa, la solución de producto es el toggle **Alto contraste** (Capa B) que sube
   `contrastLevel`.
4. **Escala de fuente 200%**: QA con fuente grande. Quitar/relajar `maxLines`+`Ellipsis` que
   recorten en pills y etiquetas; permitir 2 líneas y `wrapContentHeight`.
5. **Reducir movimiento del sistema**: leer `Settings.Global.ANIMATOR_DURATION_SCALE`; si es
   0 (o el toggle propio está activo, ver B), **atenuar/parar** pulso del orbe, animación de
   color del tema y los ticks. (Conecta con fotosensibilidad.)
6. **No depender solo del color**: el estado y el modo ya tienen **etiqueta de texto** —
   mantenerlo; no introducir señales solo-color.
7. **Foco y orden lógico** + `heading()` semántico en los títulos de sección de Ajustes.

## Capa B — Sección "Accesibilidad" en Ajustes (preferencias reales)
Pocas y con sentido. Persisten en DataStore. Toggles:

1. **Reducir destellos y animaciones** (además de respetar el sistema): atenúa el **pulso del
   orbe** y las **vistas previas** (Morse/baliza). **No** toca el flash real que el usuario
   pide a propósito. Mantiene el aviso de fotosensibilidad ya presente en el ⓘ.
2. **Alto contraste**: sube `contrastLevel` en `rememberDynamicColorScheme` (p. ej. 0.0 → 1.0)
   y/o fuerza bordes/superficies más marcadas. Resuelve el riesgo del tinte `Content`.
3. **Vibración (háptica)** al encender/apagar/cambiar de modo. Gateado por `hasVibrator`.
4. **Mostrar siempre el nombre del modo** (por si no se distinguen iconos) — ya se muestra;
   el toggle solo lo refuerza/asegura.
5. **Confirmar/bloquear antes de salir del modo** — accesibilidad **motora**. Es el sistema de
   **bloqueo/desbloqueo** ya anotado en el roadmap (empezando por Pantalla). Su interruptor
   vive aquí. (Implementación del bloqueo: hito aparte; aquí solo el ajuste.)

### v1.1+ (funciones con peso propio; su on/off vive aquí)
- **Avisos por destello de pantalla + vibración ante sonidos** (llanto de bebé, alarma de
  humo, timbre) — accesibilidad para personas sordas. Encaja con **TFLite/YAMNet**. Aquí "IA"
  sí es honesto. Permiso de micrófono + servicio en primer plano.
- **Comandos de voz** (`SpeechRecognizer`) para encender/cambiar de modo — motora/visión.

---

## Diseño técnico
- **Nuevo** `domain/model/AccessibilitySettings.kt`:
  `data class AccessibilitySettings(reduceMotion: Boolean = false, highContrast: Boolean = false, haptics: Boolean = true, alwaysShowModeName: Boolean = true, confirmExit: Boolean = false)`.
- **Nuevo** `data/settings/DataStoreAccessibilityPreferencesRepository` (mismo patrón que
  Theme), expuesto vía Hilt (`DataStoreModule`/`AppModule`).
- **Nuevo** `AccessibilityViewModel` (o ampliar `ThemeViewModel`) que emita el flujo.
- `DeviceCapabilities` **+= `hasVibrator: Boolean`** (consultar `Vibrator`/`VibratorManager`).
- **Reducir movimiento** efectivo = `prefs.reduceMotion || systemAnimatorScale == 0f`.
  Exponerlo como `CompositionLocal` (`LocalReduceMotion`) leído por PowerOrb/ticks/Theme.
- **Alto contraste** → pasar `contrastLevel` a `rememberDynamicColorScheme` en `Theme.kt`
  (firma ya lo soporta; hoy implícito 0.0). `LumiAiTheme(... highContrast: Boolean)`.
- **Háptica** → util `HapticController` (gateado por `hasVibrator`), llamado desde el motor /
  ViewModel al on/off/cambio de modo. Respetar `haptics` pref.
- **Strings**: nuevas `a11y_*` (título de sección, labels de toggles, `*_cd` que falten).
  Mantener español (no hay `values-en`).

## Tareas (bite-sized, commit por lote; verificar leyendo + Scanner/TalkBack en dispositivo)

### Lote 1 — Semántica/TalkBack (Capa A, sin opción)
- [ ] Añadir `semantics`/`stateDescription`/`liveRegion` al PowerOrb y rol a "Toca para encender".
- [ ] `contentDescription` + rol a pills, sliders, ⓘ, asa de Pantalla, swatches/chips.
- [ ] Strings `*_cd` que falten. Commit `feat(a11y): semantics & contentDescription`.

### Lote 2 — Objetivos táctiles + fuente 200%
- [ ] Áreas de toque ≥48dp (ⓘ, asa, swatches) vía `minimumInteractiveComponentSize`/`sizeIn`.
- [ ] Relajar `maxLines`/`Ellipsis` que recorten a 200%. Commit `fix(a11y): touch targets & font scaling`.

### Lote 3 — Prefs de accesibilidad (DataStore + VM + DeviceCapabilities.hasVibrator)
- [ ] `AccessibilitySettings` + repo DataStore + Hilt + ViewModel.
- [ ] `DeviceCapabilities.hasVibrator`. Commit `feat(a11y): accessibility preferences store`.

### Lote 4 — Sección "Accesibilidad" en Ajustes (UI de toggles)
- [ ] Sección nueva en `SettingsScreen` (heading) con: reducir movimiento, alto contraste,
      háptica, mostrar nombre de modo, confirmar salida (placeholder hasta el hito de bloqueo).
- [ ] Strings `a11y_*`. Commit `feat(settings): accessibility section`.

### Lote 5 — Conectar prefs al comportamiento
- [ ] `LocalReduceMotion` → PowerOrb/ticks/animación de tema atenúan/paran.
- [ ] `highContrast` → `contrastLevel` en `Theme.kt`.
- [ ] `HapticController` gateado por `hasVibrator` + pref. Commit `feat(a11y): wire reduce-motion/contrast/haptics`.

### Lote 6 — QA y verificación ✅ (QA en dispositivo hecho · 28 jun)
- [x] Pasada de **Accessibility Scanner** + **TalkBack** + fuente 200% en dispositivo.
- [x] Contraste por acento revisado (alto contraste disponible para acentos límite).
- [x] Tests de `semantics` con **Robolectric + Compose UI test** (`testDebugUnitTest`): **orbe** (apagado/encendido + click) y **ModePill** (selected true/false, rol Tab, stateDescription al bloquear). Resto opcional: nombre de modo a nivel de Hub (requiere Hilt).

## Fuera de alcance (v1.1+)
- Avisos por sonido (TFLite/YAMNet) y comandos de voz: features con su propio hito.

## Prioridad
1. ✅ **v1 (requisito Play) HECHO:** Lotes 1–5 + **QA en dispositivo (Lote 6)**.
2. ✅ **confirmar/bloquear salida HECHO:** bloqueo de Pantalla + pref "Bloquear pantalla automáticamente".
3. **v1.1+:** sonido (TFLite) y voz.
