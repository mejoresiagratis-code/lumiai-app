# Alerta Sonora — Borrador de cumplimiento (Data Safety + política)

Estado: **borrador F1**. No vinculante hasta revisión legal y QA. Sirve para preparar el
formulario de Data Safety de Play y la sección de política de privacidad antes de activar el
runtime de IA (F2+).

## Permisos declarados (F1)

- `RECORD_AUDIO` (runtime, *while-in-use*): necesario para escuchar y clasificar sonidos.
  Se solicita **solo al abrir el modo Alerta Sonora**, tras una divulgación destacada. El resto
  de la app no usa el micrófono.
- `FOREGROUND_SERVICE_MICROPHONE` (normal, Android 14+): se declara para el servicio de escucha
  que llegará en F2. El servicio combinará `microphone|specialUse`.
- `android.hardware.microphone` como `uses-feature required="false"`: la app sigue instalable en
  dispositivos sin micrófono; el modo simplemente se oculta (gating de F0).

## Qué datos se tratan

- **Audio del micrófono**: se procesa **en el dispositivo**, en memoria, en ventanas de ~1 s.
  - No se graba a disco.
  - No se envía por red (el modo no usa internet).
  - No se comparte con terceros.
  - No se asocia a la identidad del usuario.
- **Configuración del modo** (categorías activas, sensibilidad): se guardará localmente en
  DataStore (F3). No sale del dispositivo.

## Borrador de declaración Data Safety (Play Console)

- ¿Se recopila o comparte audio? **No se recopila ni se comparte.** El micrófono se usa para una
  función en el dispositivo (procesamiento en tiempo real) y el audio no se almacena ni transmite.
- Tipo de dato: Audio (micrófono) — **procesado en el dispositivo, no recopilado**.
- Cifrado en tránsito: N/A (no hay transmisión).
- Eliminación: N/A (no se almacena audio).

## Borrador de sección de política de privacidad

> **Modo Alerta Sonora.** Si activas este modo, LumiAI usa el micrófono de tu dispositivo para
> reconocer ciertos sonidos (timbre, teléfono, alarmas, llanto de bebé, etc.) y avisarte con
> destellos de luz. Todo el reconocimiento ocurre dentro de tu dispositivo mediante un modelo de
> inteligencia artificial incluido en la app. **No grabamos el audio, no lo guardamos y no lo
> enviamos a ningún servidor ni a terceros.** El micrófono solo se usa mientras este modo está
> activo, y puedes detenerlo en cualquier momento. La detección es aproximada y puede fallar:
> **no es un sistema de seguridad y no sustituye a detectores homologados de humo o monóxido de
> carbono.**

## Requisitos de Play pendientes (F5)

- Declarar el tipo de servicio en primer plano "microphone" en *Contenido de la app*, con
  justificación de accesibilidad.
- Divulgación destacada en la app antes de solicitar `RECORD_AUDIO` (implementada en F1 como la
  sección "Qué hace" de la pantalla).
- Política de privacidad publicada con la sección anterior.
- Revisar que la categoría de alarma de humo/incendio no se presente como dispositivo de seguridad.

## Notas de ingeniería

- Las etiquetas AudioSet de cada categoría (`SoundCategory.labels`) deben validarse carácter a
  carácter contra el mapa de etiquetas del modelo `.tflite` que se empaquete en F2.
- La dependencia de MediaPipe (`com.google.mediapipe:tasks-audio`) se añade en F2, no en F1, para
  no introducir un artefacto sin uso ni riesgo de resolución antes de tiempo.
