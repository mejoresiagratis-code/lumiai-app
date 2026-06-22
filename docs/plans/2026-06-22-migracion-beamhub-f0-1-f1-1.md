# Migración UI → Beam Hub · Fases 0.1 y 1.1

> Roadmap completo: ver `lumiai-roadmap-migracion-beamhub.html` (entregado al usuario).
> Regla de git: **no se hace poll del CI**; se empuja y se sigue. Verificación por lectura previa de archivos reales.

## Fase 0.1 — Preparar el terreno (hecha)
- Stack y skills recomendados documentados en el roadmap HTML.
- Trabajo de UI nuevo se construirá tras *feature flag* en fases posteriores (Beam Hub, Fase 2+).
- Bump de Compose BOM (Fase 0.2) queda **pendiente y marcado como riesgo** (salto grande desde 2024.09.03; puede requerir compileSdk/Kotlin nuevos) → se hará aparte, no dentro de esta entrega.

## Fase 1.1 — Tema claro por defecto + sistema de acento (básica)
**Principio:** extender, no borrar. Dominio intacto.

Cambios:
1. `domain/model/AccentColor.kt` (nuevo): `MULTICOLOR, AMBER, WHITE, RED, BLUE, GREEN, VIOLET`.
2. `domain/repository/ThemePreferencesRepository.kt`: añade `accentColor: Flow<AccentColor>` + `setAccentColor`.
3. `data/settings/DataStoreThemePreferencesRepository.kt`:
   - **Default de tema = LIGHT** (antes SYSTEM).
   - Persiste `accent_color` (clave nueva), default `AMBER`.
4. `ui/theme/Accent.kt` (nuevo): valores sólidos, `modeAccentColor(mode)` (paleta Multicolor), `solidColor()`, `resolveAccent(accent, mode?)`, `onColorFor(bg)`.
5. `ui/theme/Theme.kt`: `LumiAiTheme(themeMode, accent, dynamicColor, content)`. Ámbar y Multicolor conservan el esquema afinado; los demás sólidos sustituyen `primary`/`onPrimary` (mínimo viable).
6. `ui/theme/ThemeViewModel.kt`: expone `accentColor` + `setAccent`; inicial LIGHT/AMBER.
7. `MainActivity.kt`: recolecta acento y lo pasa al tema y al NavHost.
8. `ui/navigation/LumiAiNavHost.kt`: enhebra `accentColor` + `onSelectAccent` a Settings.
9. `ui/settings/SettingsScreen.kt`: sección **Color de acento** con muestras (Multicolor con degradado) + radios. Strings añadidos.

**Notas de alcance (escalado posterior):**
- Multicolor a nivel global cae a ámbar; su efecto **por modo** se aplicará en el Hub (Fase 2). Los colores **sólidos** ya afectan a toda la UI actual (botón, selección, sliders).
- Esquema armónico con MaterialKolor → Fase 1.3 (profesional).
- Animación de color al cambiar acento/modo → Fase 1.3.

**Verificación:** lectura previa de firmas reales; revisión estática (implementadores, call-sites, llaves, strings). Sin poll de CI.
