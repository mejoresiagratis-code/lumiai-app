# LumiAI · Pendientes consolidados (migración + roadmap de producto)

Fecha: 23 jun 2026 · act. 28 jun (noche) · HEAD de referencia: `3e39496`
Fuentes cruzadas: **roadmap de migración a Beam Hub** (`roadmap_src.html`) +
**roadmap/propuesta de producto** (`lumiai-roadmap-propuesta.html`), contrastadas
contra el estado **real** del repo (no contra los recuerdos ni contra los HTML, que
son del 22 jun y van por detrás).

---

## A0. Cerrado en esta sesión (posterior al consolidado original)

- ✅ **Modo Baliza v1 COMPLETO** (modo nuevo): dominio + motor de temporización, presets
  **Localización**/**Alta visibilidad** + personalizado (intervalo/destello), intensidad
  gateada, tope anti-fotosensibilidad (intervalo ≥ 400 ms), persistencia, **auto-apagado**
  (Off/15/30/60 min), **pulso del orbe** sincronizado y **variante en Pantalla** para
  móviles sin LED (parpadeo del display). → cubre además el "sin-flash → Pantalla" que era
  futuro.
- ✅ **Estado sin-flash honesto** (migración 2.2): oculta el orbe y guía a Pantalla.
- ✅ **Icono monocromo (themed icon)** + fallback mipmap API<26 + quick-wins de Lint
  (recursos sin usar, aviso de orientación suprimido).
- ✅ **Morse robusto**: normaliza acentos (á→A, ñ→N) y avisa de caracteres no soportados.
- ✅ **Aviso de fotosensibilidad en Estrobo** (parte de seguridad de Fase 1).
- ✅ **Intensidad real en Continuo CONFIRMADA** (`turnOnTorchWithStrengthLevel`, en vivo,
  gateada por `supportsTorchStrength`) — ya estaba bien.

---

## A1. Pulido de UI por acento + layout (esta sesión, HEAD `d7cf814`)

- ✅ **Tema por acento COMPLETO**: el esquema M3 generado se adopta entero, **incluidos los
  neutros** (fondo, superficie, contorno, contenedores). Toda la vista —bordes, fondos y
  detalles— refuerza la paleta del acento (Multicolor sigue al modo). Blanco→Monochrome
  (neutro real), Ámbar→TonalSpot (cálido), cromáticos→Content. Roles animados.
- ✅ **Layout full-width**: se elimina el padding lateral del contenido; el rail de modos
  sangra a los bordes; la hoja de Control pasa a padding `md`.
- ✅ **Contraste de la hoja de Control**: superficie elevada (`surfaceContainerHighest`),
  borde superior (`outlineVariant`), sombra y asa más visible → ya no se confunde con el fondo.
- ✅ **Avisos → icono de info + toast descartable**: los avisos de modo (Estrobo fotosensible,
  ayuda de Baliza) se muestran al tocar un icono ⓘ en la cabecera y se cierran al pulsar en
  cualquier parte de la pantalla (overlay de alto contraste vía `inverseSurface`).
- ✅ **Pantalla: panel de ajustes ocultable** — se colapsa dejando solo el asa superior para
  reabrirlo, maximizando la superficie de luz; tocar fuera del panel sigue saliendo del modo.
- ✅ **Presets nombrados de Pantalla** (Blanco/Cálido/Lectura/Noche) color + brillo de un toque.
- ✅ **Vista previa Morse** (puntos/rayas + duración de ciclo) para SOS y Morse de texto.
- ✅ **Beam Hub adaptativo** (consolidado aquí): hoja ≤ 42% con scroll, orbe que escala a la
  pantalla, sin solape con el botón de encendido; ritmo de espaciado 16/4 dp.

---

## A2. Cerrado en esta sesión (28 jun, tarde) — identidad, accesibilidad, cuenta y bloqueo

- ✅ **Set de iconos de marca propio** (planos, geométricos, monocromos y tintables): linterna
  como héroe, modos (sol/puntos/rayo/punto-raya/baliza), ajustes, atrás, candado, info, check;
  splash bicolor ámbar.
- ✅ **Rediseño moderno de Ajustes** (tarjetas agrupadas, tarjeta de cuenta con avatar + píldora
  de estado, segmented de tema, rejilla de swatches con anillo/check) y de **Auth** (badge de
  marca, toggle Acceder/Registro, divisor "o" + Google), con objetivos táctiles ≥48dp y semántica.
- ✅ **Hito D (cuenta/RGPD) COMPLETO**: registro/acceso, **verificación de correo** + reenvío,
  **restablecer contraseña**, **reautenticación** (password y Google), **borrar cuenta (RGPD)**
  con diálogo de confirmación, **errores en español por tipo** y vinculación de credenciales.
- ✅ **Sistema de acento ampliado**: nuevo **Amarillo** (#FFD60A, distinto del Ámbar), **tema
  oscuro por defecto** y estilo **Cálido/Vívido** (Blanco→Monochrome, Cálido→TonalSpot,
  Vívido→Content).
- ✅ **Accesibilidad Capa A+B**: semántica/roles/estado, objetivos táctiles, encabezados y
  etiquetas de sliders; **reduce-motion**, **alto contraste** (contrastLevel) y **vibración/
  háptica** (gateada por hardware). **QA en dispositivo hecho** (Scanner/TalkBack/fuente 200%/
  contraste por acento). Único resto: tests instrumentados de `semantics` (diferidos, no bloquean Play).
- ✅ **Onboarding rediseñado** con la identidad de marca (badge circular, indicador píldora,
  Atrás + Saltar con alturas estables).
- ✅ **Bloqueo de Pantalla (v1)**: candado + **overlay** que conserva el brillo y se desbloquea
  con **pulsación larga**; **pref persistida "Bloquear pantalla automáticamente"** (auto-bloqueo
  al entrar, Ajustes → Accesibilidad). **v1 cerrada** (no se extiende a otros modos, decisión de producto).

---

## A3. Cerrado en esta sesión (28 jun, noche) — Alerta Sonora (IA real) + i18n

- ✅ **Modo Alerta Sonora F0–F4 COMPLETO** (primer modo de IA real, on-device): IA de
  percepción con **YAMNet + MediaPipe Audio Classifier**, sin nube, nada se graba ni se sube.
  - **Modelo de dominio**: 8 categorías (Timbre, Golpes en la puerta, Teléfono, Perro, Llanto
    de bebé, Despertador/alarma, Sirena, Alarma de humo/incendio), cada una con etiquetas
    AudioSet, fiabilidad (alta/media) y marca de seguridad. **17 etiquetas validadas carácter a
    carácter** contra el mapa oficial de 521 clases de YAMNet (test + recurso + script).
  - **Motor de detección** (FSM pura, cooldown 4 s, ventana de debounce) + **clasificador**
    MediaPipe/YAMNet (glue confirmada compila en CI).
  - **Servicio en primer plano** (`foregroundServiceType=microphone`) que escucha y **avisa por
    canal configurable por categoría**: **Flash / Pantalla / Ambas** (cadencia por patrón
    distinto por evento). El canal Flash se oculta si el dispositivo no tiene LED.
  - **Parpadeo de pantalla** vía `ScreenFlashActivity` (full-screen-intent, despierta pantalla)
    como aviso o fallback sin flash. Permiso `USE_FULL_SCREEN_INTENT` declarado.
  - **Persistencia** en DataStore (categorías, sensibilidad y canal; codec 4 campos compatible
    hacia atrás).
  - **Gating por entitlement**: accesible con **Pro de pago O desbloqueo temporal por anuncio**;
    entrada en Ajustes → "Alerta sonora" (no en el carrusel de modos — decisión de producto).
  - **Bug de QA en dispositivo resuelto**: el flash de aviso no parpadeaba porque pasaba el
    nivel de HW crudo; ahora usa `MAX_INTENSITY` (%) como la linterna normal.
  - Resto (F5): decidir **empaquetado del `.tflite`** (en APK vs descarga), justificación de
    Data Safety (FGS-microphone + full-screen-intent), ficha por idioma. Ver
    `2026-06-28-alerta-sonora-compliance.md`.
- ✅ **i18n bilingüe EN/ES** (según guidelines de Android): `values/strings.xml` pasa a
  **inglés** (locale por defecto/fallback) y nuevo `values-es/strings.xml` con el español
  completo; **paridad exacta de 192 claves** y placeholders verificados. `res/xml/locales_config.xml`
  + `android:localeConfig` → **selector de idioma por-app de Android 13+**. Literales del feature
  de sonido extraídos a recursos (claves `sa_*` / `sound_cat_*`, mapeo único `labelRes()`
  compartido por pantalla y servicio). Strings solo-debug (God/Superusuario) quedan hardcodeadas
  a propósito (nunca se publican).
- ✅ **Proceso anti-regresión**: `docs/build-failures-memo.md` — registro de fallos de CI +
  **checklist pre-push** (receptor de `getString`/Context; impacto de cambiar el locale en tests
  que asertan texto; paridad de claves; firmas vs tests; patrones par; codec). A consultar antes
  de cada push y a ampliar tras cada fallo. (Hoy: 2 fallos de compile por `getString` en companion
  y 1 de test por literales en español al cambiar el locale → `PowerOrbSemanticsTest` ahora lee
  recursos, locale-independiente.)

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
- **Modos honestos (5 en el carrusel + 1 de IA gateado):** Continuo, Pantalla, SOS, Estrobo,
  Morse de texto (avanzado, con candado), y **Alerta Sonora** (IA real, accesible desde Ajustes,
  gateado por Pro/anuncio — ver A3). La "consolidación de modos IA engañosos" ya está hecha de
  facto: el Hub solo
  expone modos honestos (no hay Smart/Ambient/Music/Voice falsos).
- **Identidad visual #1–#8:** wordmark, etiqueta de modo, dial de ticks, línea de estado,
  hoja "Control" plegable, rail profesional, intensidad % en acento, fix de acentos
  (Blanco neutro vía Monochrome / cromáticos fieles vía Content).
- **CI:** job de calidad **no bloqueante** (ktlint + Android Lint, sube informe). Último
  Lint: **0 errores / 57 avisos** (mayoría ruido de modernización diferida).

---

## B. Pendiente de la MIGRACIÓN (solo queda nivel .2 robusta puntual y .3 profesional)

### .2 Robusta (barato, alto valor)
- ~~**Estado sin-flash**~~ → ✅ **HECHO** (orbe oculto + guía a Pantalla; ver A0).
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
- **Fase 1.5 (diseño):** ✅ **accesibilidad Capa A+B HECHA** (semántica/roles/estado, objetivos
  táctiles ≥48dp, encabezados, etiquetas de sliders; reduce-motion, alto contraste, háptica).
  ✅ **QA en dispositivo HECHO** (Accessibility Scanner, TalkBack, fuente 200%, contraste por
  acento). Resto diferido: tests instrumentados de `semantics` (no bloquean Play).
- **Fase 2 (onboarding):** ✅ **rediseño con identidad de marca HECHO** (badge, indicador
  píldora, Atrás/Saltar); "Saltar" respetado. Falta: manejar "no volver a preguntar" con
  acceso a ajustes del sistema; reanudar onboarding interrumpido.
- **Fase 3 (cuenta):** ✅ **Hito D HECHO** — vinculación de credenciales (anónima ↔ real),
  verificación de correo + reenvío, restablecer contraseña, reautenticación (password y
  Google), **borrar cuenta (RGPD)** con confirmación, errores traducidos por tipo. Falta:
  **Firebase App Check**.
- **Fase 3.5 (Morse):** ✅ acentos normalizados + aviso de no soportados + ✅ **vista previa
  punto/raya con duración de ciclo** (HECHO, ver A0/A1). Falta: contador de repeticiones;
  historial de mensajes recientes.
- **Pulido (orientación/insets):** edge-to-edge **uniforme en todas las pantallas**
  (Ajustes, Acceder); tablets en horizontal con layout adaptado.

### Pantalla v2 — huecos menores frente a la propuesta
- Núcleo hecho (presets, tono, brillo). ✅ **Presets nombrados** (Blanco/Cálido/Lectura/Noche)
  y ✅ **panel ocultable + asa para reabrir** (HECHO, ver A1). Falta: rueda de color completa
  (hoy slider de tono) y más presets (p. ej. Fiesta).
- ✅ **Bloqueo de Pantalla (v1) HECHO** + pref persistida **"Bloquear pantalla automáticamente"**:
  candado (arriba-dcha) que fija el modo encendido y **overlay** que conserva el brillo, ignora
  toques normales y **se desbloquea con pulsación larga**; auto-bloqueo opcional al entrar
  (Ajustes → Accesibilidad). UX, indicador visible y persistencia: resueltos para Pantalla.
  **v1 cerrada**; no se extiende a otros modos (decisión de producto).

### Modos nuevos (sección 03)
- ~~**Baliza / Intervalo**~~ → ✅ **IMPLEMENTADO v1 COMPLETO** (ver A0: presets, auto-apagado,
  pulso del orbe, variante en Pantalla). Resto de modos nuevos siguen sin implementar.
- **Vela / Llama** — Avanzado, medio. Ruido aleatorio sobre la intensidad (API 33+); mejor
  aún en Pantalla con tono cálido. *Candidato a v1.*
- **Pantalla multicolor / Fiesta** — Avanzado, bajo. Ciclo de color del display.
- **Sincronizado con sonido** — Avanzado, alto. Amplitud de micro en tiempo real (permiso
  de micrófono). Honesto: no es "IA" ni reconocimiento musical.
- **(Horizonte) Lectura/Sueño warm-screen.**
- **(Horizonte) Modos con IA — investigado 28 jun** (propuesta comparativa HTML: `lumiai-modos-ia.html`).
  Conclusión: en una linterna la IA útil es **on-device y de percepción**, no un LLM en la nube
  (Gemini en la nube NO sirve para tiempo real; solo encaja en el constructor por lenguaje).
  Ranking por valor real:
  1. ~~**Alerta sonora** (estrella)~~ → ✅ **IMPLEMENTADO F0–F4 COMPLETO** (ver A3). **YAMNet +
     MediaPipe Audio Classifier** escucha y avisa distinto por evento (timbre/llanto de bebé/
     alarma de humo/sirena/golpes) por Flash/Pantalla/Ambas. IA real, on-device, accesibilidad
     genuina, nada se sube. Permiso micro. minSdk 24. Gateado por Pro/anuncio. Resto: F5
     (empaquetado del modelo, Data Safety, ficha por idioma).
  2. **Manos libres** — aplauso/chasquido (audio) o gesto (MediaPipe) para encender.
  3. **Constructor por lenguaje** — "luz roja parpadeante lenta" → ajustes/patrón reales con
     **Gemini Nano** on-device (solo gama alta) + fallback nube vía **Firebase AI Logic**
     (sin clave embebida; con App Check). Único hueco donde un LLM aporta.
  4. **Música** — beat-detection **DSP** (no IA → "reactivo al sonido"); YAMNet opcional
     para confirmar "¿suena música?". Color solo en Pantalla.
  5. **Ambiente** — sensor de luz adapta brillo/color de Pantalla (no IA, sin permisos).
  Avisos: micro = `RECORD_AUDIO` (opt-in, on-device, declarar en Data Safety; choca con
  "sin permisos innecesarios" si no se cuida); nombrar honesto, sin "AI-washing";
  gatear por **DeviceCapabilities** (ocultar si no hay micro/sensor).

### Monetización (sección 04 — no implementada)
- **Fase 4 — anuncios recompensados** → `temporaryUnlocks` (2 anuncios = 1 h del avanzado
  seleccionado). Robustez: consentimiento UMP (GDPR), verificación en servidor (SSV) +
  temporizador persistente, degradado sin red, mediación AdMob.
  AdMob (cuenta): App ID `ca-app-pub-4452549520942931~7390634923`, bloque rewarded
  `ca-app-pub-4452549520942931/3592393086`. **Regla:** IDs de **TEST** de Google en debug,
  IDs reales **solo en release** (gateados por BuildConfig) — pulsar anuncios reales propios
  durante pruebas puede marcar tráfico inválido y **banear la cuenta**.
- **Fase 5 — suscripción** Play Billing v7 + modos IA. Robustez: validación en servidor +
  restaurar compras, estados reales (gracia/pausa/reembolso), IA honesta ("detección de
  sonido (TFLite)", todo en el dispositivo).

### Publicación (Fase 6)
- Ficha de tienda, **política de privacidad**, formulario de seguridad de datos, rollout
  escalonado (5% → 100%).
- ✅ **i18n bilingüe EN/ES HECHO** (ver A3: `values/` inglés por defecto + `values-es/`, paridad
  192 claves, `locales_config`). Falta: **ficha de Play por idioma** (EN/ES) y revisar capturas/
  textos de tienda en ambos idiomas.
- **Derivados de Alerta Sonora**: decidir **empaquetado del `.tflite`** (en APK/bundle vs
  descarga bajo demanda — hoy va en el repo/APK); **Data Safety** debe declarar y justificar el
  **servicio en primer plano de micrófono** y el **`USE_FULL_SCREEN_INTENT`** (accesibilidad);
  divulgación destacada antes de `RECORD_AUDIO` (ya implementada en la pantalla).
- Calidad de release: **firma de release con secretos en CI**, R8 + subida de mapping,
  pre-launch report, Crashlytics + métricas anónimas (con consentimiento).
- Assets/avisos de Lint: ✅ **icono monocromo (themed icon)** + recursos sin usar + aviso de
  orientación (HECHO, ver A0). Resto de quick-wins de Lint según informe,
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
2. ✅ **Cuenta para Play (Hito D) HECHO:** vinculación anónima ↔ real, restablecer/verificar
   contraseña, reautenticación y **borrar cuenta (RGPD)**. Pendiente menor: Firebase App Check.
3. **Assets de tienda:** icono monocromo, ficha, política de privacidad, formulario de
   seguridad de datos.
4. **Calidad de release:** firma en CI con secretos, R8 + mapping, Crashlytics, rollout por
   fases.
5. *(Opcional v1)* 1–2 modos nuevos baratos (**Baliza**, **Vela**) para enriquecer la ficha.
6. **Monetización** (anuncios → suscripción) como **v1.1**.

**Diferir:** modernización de toolchain + M3 Expressive (hito propio, alto riesgo sin build local).
