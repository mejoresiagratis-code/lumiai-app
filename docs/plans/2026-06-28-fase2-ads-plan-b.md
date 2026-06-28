# Fase 2 · Plan B si el CI sigue en rojo por metadata de AdMob

**Contexto.** `play-services-ads:25.2.0` y `user-messaging-platform:4.0.0` están
compilados con **Kotlin 2.2** (metadata 2.2.0). El proyecto usa **Kotlin 2.0.21**,
cuyo compilador solo lee metadata ≤ 2.0.0 → CI falla en `:app:kspDebugKotlin` con
*"Module was compiled with an incompatible version of Kotlin. The binary version of
its metadata is 2.2.0, expected version is 2.0.0."*

**Solución primaria (ya aplicada, commit `a4de1c2`).**
`-Xskip-metadata-version-check` en todas las tareas de compilación Kotlin (compile +
KSP). Permite leer binarios con metadata más nueva sin subir Kotlin. Solo afecta a la
lectura de dependencias; nuestro código sigue en 2.0.21. **Si el CI queda verde, no
hace falta nada más.**

Solo si el CI sigue en rojo, elegir UNA de estas dos vías.

---

## Plan B1 — Bajar AdMob + UMP a versiones pre-Kotlin-2.2 (rápido)

El SDK 24.x es el de arquitectura antigua (`com.google.android.gms.ads`,
mayoritariamente Java) y no arrastra los módulos Kotlin internos que disparan el
check. UMP 3.x es de 2024 (anterior a Kotlin 2.2) y mantiene la misma API que usa
`AdsConsentManager` (`requestConsentInfoUpdate`, `loadAndShowConsentFormIfRequired`,
`canRequestAds`), así que **no hay que tocar código Kotlin**.

`gradle/libs.versions.toml`:

```toml
playServicesAds = "24.9.0"          # antes: 25.2.0
userMessagingPlatform = "3.1.0"     # antes: 4.0.0  (usar la 3.x más alta disponible)
```

`app/build.gradle.kts`: **quitar** el bloque añadido en `a4de1c2`
(`tasks.withType<KotlinCompilationTask<*>> { ... -Xskip-metadata-version-check }`),
ya que deja de ser necesario y es más limpio sin él.

Notas:
- Si 24.9.0 aún trae metadata ≥ 2.1, bajar a una 24.x más antigua (p. ej. `24.4.0` o
  `24.2.0`), que son aún más "solo Java".
- Verificar en CI que `RewardedAd.load`, `FullScreenContentCallback`,
  `OnUserEarnedRewardListener` y `MobileAds.initialize` siguen igual en 24.x (la API
  de recompensados no cambió entre 24 y 25; las roturas de 25.0.0 fueron en mediation
  y banners adaptativos, no en rewarded).
- UMP 3.x: confirmar que `loadAndShowConsentFormIfRequired` y `canRequestAds` existen
  (sí desde UMP 2.1). Si una firma difiere, ajustar `AdsConsentManager`.

## Plan B2 — Subir el proyecto a Kotlin 2.2 (limpio a largo plazo, su propia tarea)

Es la dirección correcta de cara a publicar (mantiene AdMob 25.x + UMP 4.0.0 y elimina
el flag). Cambios mínimos en `gradle/libs.versions.toml`:

```toml
kotlin = "2.2.0"           # o la 2.2.x estable vigente
ksp = "2.2.0-<match>"      # el KSP que empareja exactamente con esa Kotlin
```

Y `app/build.gradle.kts`: quitar el bloque del flag de `a4de1c2`.

Riesgos a verificar en CI (por eso es su propia tarea, no un cambio a ciegas):
- **Hilt 2.52** con Kotlin 2.2 / KSP 2.2 (puede requerir subir Hilt).
- Plugin de Compose (`org.jetbrains.kotlin.plugin.compose`) queda atado a la versión
  de Kotlin vía el alias → sube solo, pero validar Compose BOM 2024.09.03.
- AGP 8.7.3 + Kotlin 2.2 (compatibilidad).

**Recomendación:** mantener el flag (primaria) mientras el CI esté verde; abordar B2
como tarea propia antes del release y entonces retirar el flag.
