# LumiAI · Pendientes consolidados (migración + roadmap de producto)

Fecha: 23 jun 2026 · HEAD de referencia: `8033cf9`
Fuentes cruzadas: **roadmap de migración a Beam Hub** (`roadmap_src.html`) +
**roadmap/propuesta de producto** (`lumiai-roadmap-propuesta.html`), contrastadas
contra el estado **real** del repo (no contra los recuerdos ni contra los HTML, que
son del 22 jun y van por detrás).

---

## A. Ya completado (contexto, incl. trabajo posterior a los roadmaps del 22 jun)

- **Migración Beam Hub: completa.** Hub con orbe central (enciende/apaga), dial de
  ticks, nombre de modo, línea de estado y rail de modos profesional. Hoja de cristal
  (Haze) con control contextual gateado por `ModeControls`/`DeviceCapabilities`, chips
  con candado → Auth, reset a CONTINUO al cerrar sesión, anclaje en `bottomBar` +
  `navigationBarsPadding()`.
- **Sistema de acento: completo.** `AccentColor` (Multicolor + sólidos) persistido en
  DataStore, esquema M3 armónico vía MaterialKolor, animación de color. Selector en
  **Ajustes → Apariencia** con vista previa; tema **claro por defecto** + claro/oscuro/sistema.
- **Pantalla v2: núcleo hecho.** Presets de color (incl. Cálido), slider de tono (hue),
  override de brillo real con restauración al salir, hoja que absorbe toques.
- **Modos honestos (5):** Continuo, Pantalla, SOS, Estrobo, Morse de texto (avanzado, con
  candado). La "consolidación de modos IA engañosos" ya está hecha de facto: el Hub solo
  expone modos honestos (no hay Smart/Ambient/Music/Voice falsos).
- **Identidad visual #1–#8:** wordmark, etiqueta de modo, dial de ticks, línea de estado,
  hoja "Control" plegable, rail profesional, intensidad % en acento, fix de acentos
  (Blanco neutro vía Monochrome / cromáticos fieles vía Content).
- **CI:** job de calidad **no bloqueante** (ktlint + Android Lint, sube informe). Último
  Lint: **0 errores / 57 avisos** (mayoría ruido de modernización diferida).

---

## B. Pendiente de la MIGRACIÓN (solo queda nivel .2 robusta puntual y .3 profesional)

### .2 Robusta (barato, alto valor)
- **Estado sin-flash:** hoy solo muestra aviso; falta **ocultar el orbe de LED y guiar
  explícitamente a Pantalla** (migración 2.2).
- **Indicador de batería en el Hub** (estaba en el mockup; opcional/cosmético).
- **Tests de Compose del Hub** (orbe on/off, nombre de modo, `contentDescription`) y
  **test del mapper de acento** (valor persistido → color).
- **Gesto de arrastre** para expandir/colapsar la hoja (hoy es toggle "Ver más / Ver menos").

### .3 Profesional (DIFERIDO como hito de modernización de toolchain)
- Compose BOM bump → **M3 Expressive + Material motion** + transiciones `sharedBounds`
  Hub ↔ Ajustes.
- Microinteracciones en chips, blur progresivo en el borde de la hoja, halo/anillo Morse
  más vivo (Lottie opcional).
- **Selector de acento como rueda** además de la lista.
- **Baseline de regresión visual**; QA sistemático claro/oscuro en todas las pantallas;
  rendimiento (recomposiciones/jank) y TalkBack.

> Riesgo del hito diferido: exige Kotlin 2.1+, compileSdk 36, AGP 8.9+/9.x, bump de
> Gradle y de Haze/MaterialKolor 4.x. Sin build local, alto riesgo. No es necesario para
> el look "imagen 5". Se aborda como milestone propio (plan en
> `2026-06-23-fase0-extras-evaluacion.md`).

---

## C. Pendiente del ROADMAP DE PRODUCTO (robustez por fase → camino a publicación)

Cada fase "Completada" del producto arrastra extras de **robustez** que aún no están:

- **Fase 0 (CI):** ktlint + detekt + Lint como pasos **bloqueantes** (hoy no bloqueante,
  sin detekt); cobertura JaCoCo con umbral; APK a Firebase App Distribution.
- **Fase 1 (motor/LED):** resiliencia del LED (recuperar si otra app ocupa la cámara,
  reintento + mensaje); aviso/limitación por batería baja o calor en estrobo prolongado;
  política de ciclo de vida al apagar pantalla / quitar de recientes.
- **Fase 1.5 (diseño):** accesibilidad (etiquetas TalkBack, contraste ≥ 4.5:1, fuente
  dinámica); tests de Compose de estados clave; QA de tema claro/oscuro.
- **Fase 2 (onboarding):** manejar "no volver a preguntar" con acceso a ajustes del
  sistema; reanudar onboarding interrumpido; respetar "Saltar".
- **Fase 3 (cuenta):** **vincular sesión anónima ↔ cuenta real** (no perder uid/progreso);
  cuenta completa: verificación de correo, restablecer contraseña, reautenticación,
  **borrar cuenta (RGPD)**; Firebase App Check; errores traducidos por tipo.
- **Fase 3.5 (Morse):** avisar de caracteres no soportados y **normalizar acentos (á→A)**;
  vista previa punto/raya + contador de repeticiones; historial de mensajes recientes.
- **Pulido (orientación/insets):** edge-to-edge **uniforme en todas las pantallas**
  (Ajustes, Acceder); tablets en horizontal con layout adaptado.

### Pantalla v2 — huecos menores frente a la propuesta
- Núcleo hecho (presets, tono, brillo). Falta: **set de presets honestos nombrados**
  (Blanco, Cálido, Rojo nocturno, Lectura, Fiesta), rueda de color completa (hoy slider de
  tono), y gesto para **volver a mostrar el panel** tras ocultarlo.

### Modos nuevos (sección 03 — NINGUNO implementado aún)
- **Baliza / Intervalo** — Básico, esfuerzo bajo. Temporización pura del LED (intervalo +
  duty cycle). *Candidato a v1 por barato.*
- **Vela / Llama** — Avanzado, medio. Ruido aleatorio sobre la intensidad (API 33+); mejor
  aún en Pantalla con tono cálido. *Candidato a v1.*
- **Pantalla multicolor / Fiesta** — Avanzado, bajo. Ciclo de color del display.
- **Sincronizado con sonido** — Avanzado, alto. Amplitud de micro en tiempo real (permiso
  de micrófono). Honesto: no es "IA" ni reconocimiento musical.
- **(Horizonte)** Lectura/Sueño warm-screen; **IA real on-device**: detección de sonido
  TFLite YAMNet (llanto de bebé / alarma de humo / timbre) + comandos de voz.

### Monetización (sección 04 — no implementada)
- **Fase 4 — anuncios recompensados** → `temporaryUnlocks` (2 anuncios = 1 h del avanzado
  seleccionado). Robustez: consentimiento UMP (GDPR), verificación en servidor (SSV) +
  temporizador persistente, degradado sin red, mediación AdMob.
- **Fase 5 — suscripción** Play Billing v7 + modos IA. Robustez: validación en servidor +
  restaurar compras, estados reales (gracia/pausa/reembolso), IA honesta ("detección de
  sonido (TFLite)", todo en el dispositivo).

### Publicación (Fase 6)
- Ficha de tienda, **política de privacidad**, formulario de seguridad de datos, rollout
  escalonado (5% → 100%).
- Calidad de release: **firma de release con secretos en CI**, R8 + subida de mapping,
  pre-launch report, Crashlytics + métricas anónimas (con consentimiento).
- Assets/avisos del análisis de Lint: **icono monocromo (themed icon)**, eliminar 3
  recursos sin usar, suprimir/aceptar aviso de orientación bloqueada,
  revisar `CredentialManagerSignInWithGoogle` en `AuthScreen.kt`.

---

## D. Riesgos de seguridad pendientes (de esta línea de trabajo)
- **Revocar los DOS tokens de GitHub** expuestos en el chat (aconsejado varias veces).
- **Keystore de debug commiteado** (`app/lumiai-debug.keystore`): aceptado para SHA-1
  estable en debug, pero **no usar para release**; generar keystore de release aparte y
  **fuera del repo**.

---

## E. Priorización sugerida hacia publicación (v1 mínima viable)
1. **Robustez barata de alto impacto:** estado sin-flash (ocultar orbe + guiar a Pantalla),
   normalización de acentos en Morse, edge-to-edge uniforme.
2. **Cuenta para Play:** vincular anónima ↔ real (no perder uid), restablecer/verificar
   contraseña y **borrar cuenta (RGPD)** — requisito real de tienda.
3. **Assets de tienda:** icono monocromo, ficha, política de privacidad, formulario de
   seguridad de datos.
4. **Calidad de release:** firma en CI con secretos, R8 + mapping, Crashlytics, rollout por
   fases.
5. *(Opcional v1)* 1–2 modos nuevos baratos (**Baliza**, **Vela**) para enriquecer la ficha.
6. **Monetización** (anuncios → suscripción) como **v1.1**.

**Diferir:** modernización de toolchain + M3 Expressive (hito propio, alto riesgo sin build local).
