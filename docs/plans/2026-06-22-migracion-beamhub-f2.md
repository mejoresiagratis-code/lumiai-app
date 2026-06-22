# Migración UI → Beam Hub · Fase 2 (básica, tras feature flag)

## Qué entra
- `ui/Features.kt`: `Features.BEAM_HUB` (true). Revertir = ponerlo a false (vuelve el grid clásico).
- `ui/home/beamhub/BeamHubScreen.kt` (nuevo): pantalla orbe-céntrica.
  - **Fondo por modo:** degradado del color de acento activo (que ya viene del tema de las Fases 1.2/1.3) hacia el fondo de marca; más intenso con la linterna encendida.
  - **Orbe central** de encender/apagar (`PowerOrb`): círculo pulsador con halo y micro-animación (escala/halo con `animateFloatAsState`). Sin desenfoque (Haze llega en la fase robusta).
  - **Reutiliza** `ModeGrid` (selector), `ModeSettingsPanel` (controles del modo) y `ScreenLight` (modo Pantalla, con su early-return y BackHandler) — dominio y lógica intactos.
- `ui/navigation/LumiAiNavHost.kt`: la ruta HOME elige Beam Hub o `HomeScreen` según el flag. `HomeScreen` se conserva intacto.

## Principio aplicado
Extender, no borrar: la pantalla antigua queda como fallback detrás del flag; cero cambios en dominio/repos/ViewModel. Un commit de feature.

## Escalado posterior (no en esta entrega)
- **Robusta:** hoja de controles deslizante tipo cristal (Haze), centrado vertical real del orbe, transición de fondo animada entre modos.
- **Profesional:** orbe con anillo de progreso/animación de haz, iconografía de energía, retirada del grid clásico una vez verificado.

## Verificación (sin poll de CI)
Firmas reales leídas (ModeGrid/ModeSettingsPanel/ScreenLight idénticas a HomeScreen); APIs M3 estándar (topAppBarColors, animateFloatAsState, scale, radialGradient, clickable(role)); llaves balanceadas; HomeScreen intacto.
