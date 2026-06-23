# Fase 0 · Extras — evaluación y decisión (23 jun 2026)

Estado: la migración Beam Hub está funcional en dispositivo (HEAD b0976e8). Quedan los
"extras" de Fase 0: **0.2 (subir Compose BOM)** y **0.3 (M3 Expressive + Material motion,
ktlint/detekt/Lint en CI, baseline de capturas)**.

## Mapa real de versiones (consultado en Maven, jun 2026)
| Compose BOM   | compose ui | material3 | Toolchain requerido |
|---------------|-----------|-----------|---------------------|
| 2024.09.03 (actual) | 1.7.0 | 1.3.x | Kotlin 2.0.21 · SDK 35 · AGP 8.7.3 (actual) |
| 2025.01.01    | 1.7.7 | 1.3.1 | igual (línea Compose 1.7) |
| 2025.05.00    | 1.8.1 | 1.3.2 | igual toolchain; **sin** M3 Expressive |
| 2026.06.00    | 1.11.3 | 1.4.0 (Expressive) | Kotlin 2.1 · compileSdk 36 · AGP 8.9+/9.x · Gradle nuevo |

Otras estables: Haze 1.7.2 (2.0 en alpha) · MaterialKolor 4.1.1 (5.0 alpha) · AGP serie 9.x.

## La cascada del bump grande (para M3 Expressive)
material3 1.4 vive en BOMs 2026.x (compose ui 1.11), que arrastran:
- Kotlin 2.0.21 → 2.1.x (+ KSP correspondiente + Hilt compatible)
- compileSdk/targetSdk 35 → 36 (Android 16)
- AGP 8.7.3 → 8.9+/9.x  + Gradle 8.9 → versión mayor
- Haze 1.4 → 1.7/2.0 · MaterialKolor 2.1 → 4.x
Riesgo alto y **sin build local** para verificar. M3 Expressive **no es necesario para la imagen 5**.

## Opciones
- **A · Gate de CI (recomendado, ya):** Android Lint (no bloqueante) + ktlint/detekt con
  baseline o `ignoreFailures` para no inundar RED. Aporta valor real y, como no compilo aquí,
  ayuda a cazar errores. Bajo riesgo.
- **B · Bump conservador a Compose 1.8 (BOM 2025.05.00):** mantiene Kotlin 2.0.21 / SDK 35 /
  AGP 8.7.3. Nos saca de la BOM de 2024 sin terremoto. Requiere verificar que Haze y
  MaterialKolor elegidos sean de la línea Compose 1.8 (sus .module KMP). No da Expressive.
- **C · Modernización completa (Expressive):** la cascada de arriba. Aplazar a milestone propio.

## Decisión propuesta
1. Cerrar la parte de valor de 0.3 con **A** (gate de CI) ahora.
2. Documentar **C** como milestone "Modernización toolchain" (este archivo).
3. **B** opcional si se quiere salir ya de la BOM 2024 (intermedio de bajo riesgo).
4. **Saltar** M3 Expressive + Material motion: no aportan al look de la imagen 5.
