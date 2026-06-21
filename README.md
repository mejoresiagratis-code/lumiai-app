# LumiAI (starter pack)

Linterna Android (Kotlin + Jetpack Compose) construida desde cero, limpia y publicable.

## Estado
- **Fase 0** — Esqueleto que compila + CI de debug en verde. (actual)

## Stack
- Kotlin 2.0.21 · AGP 8.5.2 · Gradle 8.9
- Jetpack Compose (Material 3) · Navigation Compose
- Hilt (DI) · DataStore · Coroutines
- Core SplashScreen API
- minSdk 24 · targetSdk 34

## Arquitectura
Por capas (`data` / `domain` / `ui`). Regla de oro: **una sola clase tocará el LED**
para evitar conflictos al cambiar de modo (lección del repo anterior).

## Build local
```bash
./gradlew assembleDebug
```
