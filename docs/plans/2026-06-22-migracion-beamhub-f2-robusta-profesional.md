# Beam Hub · Fase 2 robusta + profesional

## Robusta (commit aparte)
- Dependencia **Haze 1.4.0** (`dev.chrisbanes.haze:haze`). Pin elegido por compatibilidad: es la 1.x más alta en la **línea Compose 1.7** (ui-android 1.7.8); las 1.5+ saltan a Compose 1.8. La Compose BOM (`platform`) restringe el Compose transitivo a 1.7.2 (API estable entre parches). API leída en el tag 1.4.0: `HazeState()`, `hazeSource(state)`, `hazeEffect(state, style)`, `HazeDefaults.style(backgroundColor, blurRadius)`.
- Layout reestructurado: fuente del blur a pantalla completa (gradiente por modo) marcada con `hazeSource`; Scaffold transparente; **orbe centrado vertical** entre la barra superior y la **hoja de cristal** inferior (`hazeEffect`) que difumina el fondo por modo y contiene `ModeSettingsPanel` (scroll interno, alto máximo). Fondo animado: el `primary` ya transiciona por modo gracias al tema (Fase 1.3).

## Profesional (commit aparte)
- `PowerOrb` mejorado, sin dependencias ni drawables nuevos (todo con `Canvas`):
  - **Anillo/haz**: pista tenue + arco giratorio (`rememberInfiniteTransition` + `animateFloat`, `tween` lineal de 2.6 s) que solo gira con la linterna encendida.
  - **Glifo de energía** dibujado (arco con hueco arriba + línea vertical) en lugar de texto; etiqueta accesible vía `onClickLabel`.
  - Halo y micro-escala animados con `animateFloatAsState`.

## Principio: extender, no borrar
El grid clásico (`HomeScreen`/`ModeGrid`/`ModeCard`/`ModeSettingsPanel`) **sigue intacto** tras el flag `Features.BEAM_HUB`. La retirada del grid se hará **solo tras verificar** en verde/dispositivo (no a ciegas).

## Verificación (sin poll de CI)
APIs de Haze leídas en el tag 1.4.0; APIs de Canvas/animación estándar de Compose; imports vs uso comprobados; llaves balanceadas; HomeScreen intacto.
