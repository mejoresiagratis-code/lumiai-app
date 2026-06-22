# Migración UI → Beam Hub · Fases 1.2 y 1.3

## Fase 1.2 — Multicolor en vivo (modo activo → tema)
- `ThemeViewModel` inyecta `FlashStateRepository` y expone `currentMode: StateFlow<FlashMode>` (inicial CONTINUOUS).
- `MainActivity` recolecta `currentMode` y lo pasa como `activeMode` a `LumiAiTheme`.
- `LumiAiTheme(themeMode, accent, activeMode, …)`: con acento **Multicolor**, el color sigue al modo activo vía `resolveAccent(accent, activeMode)` (CONTINUOUS=ámbar, SCREEN=azul, SOS=rojo, STROBE=violeta, TEXT=turquesa). Ámbar conserva el esquema afinado.
- Sin dependencias nuevas. Commit independiente para revertir aparte de 1.3.

## Fase 1.3 — Esquema armónico (MaterialKolor) + animación
- Dependencia: `com.materialkolor:material-kolor:2.1.1` (catálogo + `build.gradle`).
  - **Por qué 2.1.1:** es la línea Compose **1.7.3** (= AndroidX Compose 1.7.x / Material3 1.3.x), compatible con nuestra **Compose BOM 2024.09.03**. Las 3.x saltan a Compose 1.8 y las 4.x a 1.9 → arrastrarían un runtime más nuevo y romperían contra la BOM. La BOM (`platform`) además restringe los `androidx.compose.*` transitivos, manteniendo todo alineado.
  - Riesgo residual menor: `kotlin-stdlib` transitivo 2.1.20 > 2.0.21 del proyecto → posible *warning* de versión de stdlib, sin romper.
- `LumiAiTheme`: genera el esquema M3 desde el color semilla con `rememberDynamicColorScheme(seedColor, isDark)`; **anima** los roles de acento (primary/secondary/tertiary + containers) con `animateColorAsState`; **conserva los neutros** afinados de la marca (fondo/superficie/contorno) para no alterar la identidad (oscuro frío / claro limpio). El cambio de acento o de modo (Multicolor) transiciona suave.

## Verificación (sin poll de CI)
- API de MaterialKolor 2.1.1 confirmada leyendo el código en el tag 2.1.1 (`rememberDynamicColorScheme` usa `isDark`).
- Llaves balanceadas; catálogo/deps verificados; `FlashStateRepository` ya bindeado en DI.

## Siguiente
- Fase 2 (Beam Hub: orbe central + fondo por modo, tras feature flag) — entrega grande.
- Fase 0.2 (subir Compose BOM) sigue **diferida y marcada como riesgo**, aparte.
