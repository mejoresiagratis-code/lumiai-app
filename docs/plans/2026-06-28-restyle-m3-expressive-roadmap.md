# Lavado de cara M3 Expressive — Roadmap (mapa de rutas)

> **Objetivo:** que LumiAI luzca como la propuesta visual (estilo Material 3 Expressive:
> formas que se transforman, movimiento con muelle, superficies tonales) **sin cambiar ninguna
> función**. Es un *re-skin* sobre el código que ya funciona. El color por defecto es
> **ámbar sobre negro, como el Splash** (el azul del primer mockup era solo demostración del
> estilo; la marca se mantiene ámbar/negro y el usuario puede cambiar de acento).

**Arquitectura del re-skin:** se tocan SOLO la capa de presentación (Modifiers, formas, color,
motion, contenedores). **No se tocan** ViewModels, repositorios, navegación, DataStore ni lógica
de negocio. Cada fase reutiliza el composable que ya existe y le cambia la piel.

**Reglas de oro (válidas en todas las fases):**
1. **Mantener todas las funciones.** Si una fase implicara *borrar* un composable, fichero,
   string o capacidad → **PREGUNTAR antes** (no se borra nada sin confirmación de Pablo).
2. **Reutilizar lo que funciona.** Antes de escribir, leer el fichero implicado y partir de él.
3. **No romper accesibilidad ni tests.** `contentDescription`/`stateDescription` siguen saliendo
   de `R.string.*` (p. ej. `PowerOrbSemanticsTest` lee recursos; el re-skin no cambia textos).
4. **Pasar el checklist** de `docs/build-failures-memo.md` antes de cada push; CI lo confirma Pablo.
5. **Honestidad intacta:** LED monocromo; color/brillo solo reales en modo Pantalla.

**Bucle por fase (esto cumple "un ejemplo visual conforme avanzan las fases"):**
`mockup HTML ámbar/oscuro de la superficie` → Pablo aprueba → `re-skin del composable` →
`CI verde` → siguiente fase. Los mockups viven en `docs/mockups/` y se enlazan abajo.

---

## Estrategia de dos vías (clave para el riesgo)

El “M3 Expressive” real (APIs `MaterialShapes`/shape-morph, `ButtonGroup`, indicadores de carga
expresivos, tokens de motion) vive en **Compose Material3 1.4-alpha+/1.5**, que exige subir
toolchain (Kotlin 2.1+, AGP 8.9+, compileSdk 36, BOM 2025.x). Sin build local eso es alto riesgo.
Por eso se separa:

- **Vía A — “Expressive look” con el toolchain ACTUAL (riesgo bajo).** Se consigue ~80% del
  aspecto con Compose estable 1.3: tokens de **forma** propios, **motion `spring`** (ya en Compose
  Foundation), jerarquía de **superficies tonales** (ya adoptamos el esquema completo), **escala
  tipográfica** expresiva y *shape-morph* manual (animar `RoundedCornerShape`/`clip`). **Fases 0–5.**
- **Vía B — M3 Expressive REAL (gated, alto riesgo).** Bump de toolchain + APIs nativas Expressive.
  **Fase 6**, opcional, su propio hito, verificada en CI por Pablo.

Esto respeta “usar el código actual que funciona”: el grueso del lavado de cara se hace en la Vía A
sin tocar versiones.

---

## Skills por fase (programadas en el roadmap)

| Skill | Origen | Uso |
|---|---|---|
| `frontend-design` | local (anthropics) | dirección visual, tipografía, jerarquía |
| `ui-ux-pro-max` | local | tokens, estados, layout, componentes |
| `impeccable` | local | crítica/pulido fino de cada pantalla |
| `android-kotlin` | local | Compose/Modifiers/animación correctos en Kotlin |
| `writing-plans` | local | este roadmap y los sub-planes por fase |
| `webapp-testing` + `playwright-cli` | local | regresión visual de los mockups HTML |
| **`chrisbanes/skills@compose-animations`** | a instalar (autor de Haze, ya usado en la app) | motion/spring del orbe y transiciones |
| **`albermonte/android-skills@material-3-expressive`** | a instalar | solo en Fase 6 (APIs Expressive reales) |

Instalación sugerida (cuando toque su fase):
`npx skills add chrisbanes/skills@compose-animations -g -y` ·
`npx skills add albermonte/android-skills@material-3-expressive -g -y`
(Se instalan al llegar a su fase, no antes, para no meter ruido.)

---

## Fase 0 — Sistema de tokens (Vía A · riesgo bajo)

**Meta:** fijar los tokens de forma, motion, superficie y tipografía del estilo Expressive,
reutilizables por todas las pantallas. Sin tocar lógica.

**Ficheros:**
- Modificar: `ui/theme/Shape.kt` (escala de forma Expressive: sm 12 / md 18 / lg 28 / xl 40 / full).
- Crear: `ui/theme/Motion.kt` (tokens `spring()` — emphasized/standard; duraciones y `dampingRatio`).
- Modificar: `ui/theme/Type.kt` (escala expresiva: display más grande/contrastado, pesos).
- Revisar (no cambiar lógica): `ui/theme/Theme.kt` (ya adopta el esquema tonal completo).

**Skills:** `frontend-design`, `ui-ux-pro-max`, `android-kotlin`.
**Entregable visual:** `docs/mockups/2026-06-28-hub-ambar-m3.html` (Hub ámbar/oscuro) — **ya creado**.
**Riesgo:** bajo. **Borra algo:** no.

---

## Fase 1 — Beam Hub (pantalla héroe)

**Meta:** el orbe de encendido como firma (forma cuadrado-redondeado → círculo al encender, con
muelle y halo), rail de modos con chip seleccionado en *pill* tonal, hoja de Control sobre
superficie tonal. **La lógica de `FlashViewModel` no se toca.**

**Ficheros:**
- Modificar: `ui/home/beamhub/BeamHubScreen.kt` (PowerOrb: animar `RoundedCornerShape` y `scale`
  con tokens de `Motion.kt`; halo con `drawBehind`; mantener `contentDescription`/`stateDescription`
  desde `R.string`).
- Modificar: `ui/home/components/ModeUi.kt` (chips del rail: forma/elevación tonal por selección).
- Reutilizar tal cual: `ui/home/FlashViewModel.kt`, `FlashUiState.kt`.

**Skills:** `chrisbanes/skills@compose-animations` (instalar), `android-kotlin`, `impeccable`.
**Entregable visual:** `docs/mockups/.../hub-*.html`.
**Riesgo:** medio (animación). **Borra algo:** no. **Tests:** `PowerOrbSemanticsTest` debe seguir
verde (no cambiar los recursos de semántica).

---

## Fase 2 — Hoja de Control + ajustes de modo

**Meta:** sliders expresivos (track tonal, handle grande), segmented expresivos, contenedores
tonales en los ajustes contextuales por modo.

**Ficheros:**
- Modificar: `ui/home/components/ModeSettingsPanel.kt`.
- Modificar: `ui/home/components/ScreenLight.kt` (panel de Pantalla; respetar que el color sea real
  solo aquí).

**Skills:** `ui-ux-pro-max`, `android-kotlin`, `impeccable`.
**Entregable visual:** mockup de la hoja de Control. **Borra algo:** no.

---

## Fase 3 — Ajustes + Cuenta

**Meta:** tarjetas expresivas, list-items con superficie tonal, segmented de tema, rejilla de
swatches con anillo/check, tarjeta de cuenta. Sin tocar `AccountViewModel`/`ThemeViewModel`.

**Ficheros:**
- Modificar: `ui/settings/SettingsScreen.kt`.

**Skills:** `impeccable`, `ui-ux-pro-max`, `android-kotlin`.
**Entregable visual:** mockup de Ajustes. **Borra algo:** no (si algún bloque debug sobra, PREGUNTAR).

---

## Fase 4 — Auth + Onboarding

**Meta:** badge de marca, toggle Acceder/Registro, divisor “o” + Google, indicador píldora del
onboarding, todo con el lenguaje Expressive. Sin tocar `AuthViewModel`/`OnboardingViewModel`.

**Ficheros:**
- Modificar: `ui/auth/AuthScreen.kt`.
- Modificar: `ui/onboarding/OnboardingScreen.kt`.

**Skills:** `frontend-design`, `android-kotlin`, `impeccable`.
**Entregable visual:** mockup Auth + Onboarding. **Borra algo:** no.

---

## Fase 5 — Alerta Sonora

**Meta:** aplicar el estilo a las tarjetas/segmented del modo de IA (ya en Material3), coherente
con el resto. Sin tocar el servicio ni el motor.

**Ficheros:**
- Modificar: `ui/sound/SoundAlertScreen.kt`.

**Skills:** `android-kotlin`, `impeccable`.
**Entregable visual:** mockup Alerta Sonora. **Borra algo:** no.

---

## Fase 6 — (GATED · opcional · alto riesgo) M3 Expressive REAL

**Meta:** sustituir el *look* manual por las APIs nativas Expressive (shape-morph `MaterialShapes`,
`ButtonGroup`, indicadores de carga expresivos, tokens de motion oficiales).

**Prerequisitos (toolchain):** Kotlin 2.1+, AGP 8.9+, compileSdk 36, Compose BOM 2025.x
(Material3 1.4-alpha+/1.5), revisar Haze/MaterialKolor compatibles. Bump de Gradle.

**Ficheros:**
- Modificar: `gradle/libs.versions.toml` (versiones), `app/build.gradle.kts`.
- Re-skin incremental de los composables de Fases 1–5 a APIs Expressive nativas.

**Skills:** `albermonte/android-skills@material-3-expressive` (instalar), `android-kotlin`,
`webapp-testing`/`playwright-cli` (regresión).
**Riesgo:** ALTO sin build local → **un PR por bump**, CI verde obligatorio antes de seguir; si
algo no compila, revertir el bump y quedarse en Vía A. **Borra algo:** posibles helpers de forma
manuales (Fase 0) → **PREGUNTAR** antes de borrarlos.

---

## Fase 7 — QA visual y regresión

**Meta:** evitar regresiones de aspecto entre fases.

**Ficheros:** mockups en `docs/mockups/` como referencia; opcional screenshot-test de Compose.
**Skills:** `webapp-testing`, `playwright-cli`, `chrisbanes/skills@compose-animations`.
**Borra algo:** no.

---

## Self-review (cobertura del encargo)

- ✅ Mapa de rutas profesional y por fases con ficheros exactos.
- ✅ Aspecto como la propuesta (estilo Expressive) — Vía A sin riesgo + Vía B gated.
- ✅ Mantiene TODAS las funciones (solo capa de presentación; lógica intacta).
- ✅ “Preguntar antes de borrar” como regla y marcada por fase.
- ✅ Skill(s) por paso y skills a instalar **programadas** en el roadmap.
- ✅ Ejemplo visual por fase (bucle mockup→aprobación→re-skin).
- ✅ Color por defecto ámbar/negro como el Splash (ya aplicado en código).

## Handoff

Plan guardado. Ejecución sugerida: **fase a fase**, empezando por la Fase 0/1 (mockup ámbar ya
disponible). Cuando apruebes el mockup de una fase, hago el re-skin de esa fase reutilizando el
composable existente y te paso a confirmar CI. ¿Empezamos por la Fase 1 (Beam Hub)?

---

## Registro de ejecución (28 jun 2026)

**Vía A (toolchain actual) — COMPLETA.** Cada fase: re-skin solo de presentación,
sin tocar lógica, sin borrar nada, con verificación estática (balances, imports,
claves `R.string`, semántica/tests intactos) y un mockup ámbar/oscuro de ejemplo.

| Fase | Alcance | Commit(s) | Mockup |
|------|---------|-----------|--------|
| Backup | rama `backup/pre-m3-restyle` + tag `backup-pre-m3-restyle-20260628` | `6fe16df` | — |
| F0 | Tokens (Shape/Motion/Type) | `20c5d89` | — |
| F1 | Beam Hub (orbe shape-morph + rail) | `6116b93` | hub-ambar |
| F2 | Hoja de Control + ajustes de modo; panel Pantalla como hoja | `b1cab42`, `c3ae77a` | control-sheet-ambar-f2 |
| F3 | Ajustes (tarjetas, cabeceras eyebrow, swatches con muelle) | `a9c4ee0` | settings-ambar-f3 |
| F4 | Auth (badge squircle) + Onboarding (indicador con muelle) | `1396e6a`, `6df35f5` | auth-onboarding-ambar-f4 |
| F5 | Alerta Sonora (tarjetas 28dp + cabeceras eyebrow) | `9eaba66` | sound-alert-ambar-f5 |

**Principios aplicados en todas:** semántica de accesibilidad intacta (`contentDescription`,
`stateDescription`, `heading()`, `Role.*`); `PowerOrbSemanticsTest` y `ModePillSemanticsTest`
siguen válidos; imports huérfanos retirados (`CircleShape` en Auth/Onboarding); muelles vía
`LumiMotion` (emphasized/standard/effects); escala de forma de F0 reutilizada (18/28/40dp).

### Pendiente
- **F6 — Vía B (GATED, alto riesgo):** requiere bump de toolchain (Kotlin 2.1+, AGP 8.9+,
  compileSdk 36, BOM 2025.x / Material3 1.4+). Sustituye los muelles/formas manuales por las
  APIs reales de M3 Expressive (botones/segmented/slider expresivos, `MaterialShapes`,
  motion scheme). **No iniciar sin decisión explícita**: no se puede validar en sandbox y
  arrastra el retiro de `-Xskip-metadata-version-check` y posibles roturas de dependencias.
- **F7 — QA visual en dispositivo (responsabilidad de Pablo):** verificar orbe, rail, hoja de
  Control, Pantalla, Ajustes, Auth/Onboarding y Alerta Sonora en ámbar/oscuro y en claro.
