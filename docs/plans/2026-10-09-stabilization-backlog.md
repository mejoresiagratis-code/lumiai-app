# LumiAI — roadmap de estabilización

Base de auditoría: `3afc50dc8b41a62f5aa5deaee99a28fcdd710c42` (main).
Objetivo: completar la etapa 1 y los ocho P1 antes de ampliar distribución.
Validación: compilaciones y pruebas exclusivamente en GitHub Actions; QA de dispositivo por el propietario.

## Estado actualizado · 9 de octubre de 2026

Leyenda: `[x]` y tachado = implementado; `[ ]` = pendiente. Implementación, validación en Actions e integración se indican por separado. Un punto implementado no certifica las pruebas de hardware o de Play pendientes.

### Implementado e integrado en main · PR #2

- [x] ~~CI con suite completa, Android Lint en PR y release dependiente de ambos controles.~~
- [x] ~~Compras: comprobar reconocimiento, reintentar y recuperar compras sin reconocer al refrescar.~~
- [x] ~~Suscripciones: refrescar al volver a la app y permitir reconexión de Billing.~~
- [x] ~~Acceso Pro: comprobar antes de usar hardware, detener sesiones al caducar y cerrar LED.~~
- [x] ~~Captura: comunicar fallos y procesar ventanas vacías para reiniciar la detección.~~
- [x] ~~Música: escuchar sin solicitar foco que pause el reproductor.~~
- [x] ~~Pruebas de regresión, Actions y confirmación general del propietario para la primera entrega.~~
- [x] ~~Integrar PR #2 en main; build de main #249 correcta, incluida release.~~

### Implementado en PR #3 · pendiente de integración

- [x] ~~Coordinador exclusivo de flash/micrófono y bloqueo de comandos de sesiones antiguas.~~
- [x] ~~Esperar la limpieza del modo anterior antes de iniciar el siguiente.~~
- [x] ~~Vincular captura y destellos a la sesión; evitar paradas tardías de servicios antiguos.~~
- [x] ~~Música: acumular lecturas no bloqueantes, comprobar permiso al abrir y esperar al pulso anterior.~~
- [x] ~~Sonido: cancelar patrones anteriores y esperar al lector antes de cerrar MediaPipe.~~
- [x] ~~Añadir nueve pruebas de regresión.~~
- [x] ~~Validar código 2395def en Actions #252: 156 pruebas, cero fallos, errores u omitidas; Lint correcto.~~
- [x] ~~Entregar APK 0.9.54-stabilization.2, archivo lumiai-debug-252.zip.~~
- [ ] Probar el APK en dispositivo: Música ↔ Sonido ↔ Linterna, cambios rápidos, permisos, caducidad y segundo plano.
- [ ] Integrar PR #3 después de la confirmación del propietario.
- [ ] Propagar y mostrar errores de Camera2; completar QA Pixel/Samsung para cerrar STAB-04.

Evidencias: [PR #2](https://github.com/mejoresiagratis-code/lumiai-app/pull/2), [Actions main #249](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37947170674), [PR #3](https://github.com/mejoresiagratis-code/lumiai-app/pull/3) y [Actions #252](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37949146999). La validación #252 corresponde al código anterior a esta actualización exclusivamente documental.

### Incidencia de QA · Samsung S26 Ultra · entrega 2

El propietario confirma que Música recibe audio y que Sonido se detiene correctamente, pero Alerta Sonora no reconoce timbre/golpes y su notificación no abre al tocarla. El fallo se reproduce tanto con acceso de depuración como con Pro temporal. **PR #3 sigue pendiente de integración.**

- [x] ~~Corregir el ensamblado de audio: PCM16/MIC, ventanas completas de 975 ms con 50 % de solapamiento y clasificación síncrona serializada.~~
- [x] ~~Mostrar nivel de micrófono, contador de resultados y clases sin ocultarlas por umbral/allowlist.~~
- [x] ~~Añadir aviso si pasan 15 segundos sin resultados; notificación con apertura de Alerta Sonora y acción Parar.~~
- [x] ~~Añadir pruebas de lecturas parciales/solapamiento, entrada de MediaPipe, etiquetas del modelo empaquetado y acciones de notificación.~~
- [ ] Validar esta corrección en Actions y entregar APK `0.9.54-stabilization.2.1`.
- [ ] Repetir QA en Samsung S26 Ultra: comprobar que el contador avanza, el nivel varía y timbre/golpes producen clasificación y aviso. El reconocimiento físico aún no está confirmado; no se han bajado umbrales a ciegas.

### QA posterior · selección de Flash / Pantalla / Ambas

El propietario confirma que `stabilization.2.1` ya clasifica audio y dispara el flash en Samsung S26 Ultra. El clip de puerta aporreada se confunde con disparos/explosiones: queda como limitación de clasificación, sin remapear esas clases a puerta. Actions #254: 165 pruebas correctas y Lint correcto.

El propietario detecta que las tres opciones usan flash. La causa es el fallback del servicio al faltar permiso de pantalla completa, que además bloqueaba la apertura con la app visible.

- [x] ~~Respetar estrictamente las salidas seleccionadas; Pantalla nunca recurre al LED.~~
- [x] ~~Abrir la pantalla desde una Activity visible, independientemente del permiso de notificación a pantalla completa.~~
- [x] ~~En segundo plano usar notificación: su toque abre los destellos; apertura automática sujeta al permiso y al sistema.~~
- [x] ~~Añadir Probar pantalla, acceso a ajustes de permisos y avisos visibles sin detener la escucha.~~
- [ ] Validar `stabilization.2.2` en Actions y entregar APK.
- [ ] En S26 Ultra: Flash → solo LED; Pantalla → blanco/negro sin LED; Ambas → ambos. Repetir con app visible, en segundo plano y bloqueada.
- [ ] Mantener PR #3 sin integrar hasta confirmar esta matriz. Evaluar por separado el reconocimiento de golpes de puerta.

### Pendiente · orden de trabajo

1. [ ] Cerrar la segunda entrega: QA del APK y posterior integración del PR #3.
2. [ ] **STAB-02 / P1:** proteger main con PR y checks obligatorios. Requiere acceso administrativo; el workflow por sí solo no protege la rama.
3. [ ] **STAB-03 / P1:** borrado seguro de cuenta y datos en Firebase, con estado persistente, reintentos y bloqueo de sincronizaciones que puedan recrearlos. Validar con emuladores y entorno de pruebas antes de desplegar.
4. [ ] **STAB-05/06 / P1:** completar validación real de Billing con cuentas de prueba de Play; definir autoridad de suscripción, política offline y verificación backend/RTDN.
5. [ ] **STAB-04 / P1:** completar errores de cámara observables y QA físico pendiente.
6. [ ] **P2 restantes:** persistencia, consentimiento, reloj, LED, reglas, dependencias y cadena de suministro del informe. Desglosar cada cambio antes de implementarlo.
7. [ ] **Mantenimiento CI:** adoptar comprobación de formato bloqueante; el ktlint opcional retirado no equivale a tener este control.
8. [ ] **Candidato release:** pruebas físicas de MediaPipe con R8, compatibilidad 16 KB, permisos y batería; verificar artefacto y firma del candidato final. Compilar release no acredita estas pruebas.
9. [ ] **Publicación:** validar pista interna de Play y autorizar la publicación después de cerrar los criterios anteriores.

## Entrega 1: integrada y validada

- [x] ~~CI: suite JVM completa, incluidas seis pruebas Compose antes excluidas; Android Lint bloqueante en PR; reportes conservados aunque fallen los checks; release depende de ambos jobs.~~
- [x] ~~H03: comprobar acknowledge, reintentar con espera acotada y comunicar éxito después de su confirmación. Las compras no reconocidas se recuperan al refrescar; no se implementa aquí un backend de compras.~~
- [x] ~~H04: refrescar Billing en `onResume` y permitir operaciones que activen la reconexión automática.~~
- [x] ~~H05: denegar acceso inicial a servicios sin Pro, revocar sesiones y cerrar el display LED al caducar.~~
- [x] ~~H06: notificar los fallos síncronos de captura periódica y suprimir errores por parada voluntaria.~~
- [x] ~~H07/H08 asociados: no tomar foco de reproducción para escuchar música; procesar ventanas vacías para reiniciar rachas.~~
- [x] ~~Pruebas de regresión para reconocimiento, autorización/revocación, executor y ventanas vacías.~~
- [x] ~~Actions [37939256628](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37939256628) correcto y QA confirmado por el propietario («Todo ok probado»). PR #2 integrado en `7e5bbcfcb2321567a097fc026531ab0d0a5d556c`.~~

La entrega 2 queda en PR para validar el APK en dispositivo. No despliega Firebase ni modifica reglas/protección de GitHub.

## Tareas ejecutables

### STAB-01 · P1 · Controles obligatorios de CI (H15)

**Responsable:** Android/DevOps. **Dependencias:** ninguna. **Estimación:** 1–2 días.

**Alcance:** pruebas completas, lint en PR, artefactos y diagnósticos, dependencias de release.
**Criterios de aceptación:** un test o error de lint marca rojo el check; release no corre si falla cualquiera; resultados JUnit/lint disponibles; APK debug se entrega solo después de superar las pruebas.
**Estado:** integrado en PR #2 y validado en Actions. El control obligatorio de integración se completa con STAB-02. Se retira el ktlint opcional que siempre terminaba en verde; su adopción bloqueante requiere una tarea separada de formato, sin fingir que ya está aplicado.

### STAB-02 · P1 · Proteger main (H16)

**Responsable:** administrador del repositorio. **Dependencias:** STAB-01 con primera ejecución correcta. **Estimación:** 30–60 minutos.

**Alcance:** ruleset para main, exigir PR y los checks `Tests and debug APK` y `Android Lint` tras comprobar sus nombres publicados. Evitar bypass ordinario y force push; documentar recuperación de emergencia.
**Criterios de aceptación:** PR con check rojo no integrable; push directo no autorizado rechazado. Ajustar revisiones al equipo real para no bloquear al único mantenedor exigiéndose aprobación propia.
**Estado:** pendiente. El conector disponible no ofrece escritura de branch protection/rulesets; no se afirma que el YAML proteja la rama.

### STAB-03 · P1 · Borrado idempotente y sincronización (H01)

**Responsable:** Android + backend. **Dependencias:** STAB-01; acceso al entorno Firebase de pruebas. **Estimación:** 3–5 días.

**Alcance:** estado persistente «eliminándose» por UID; detener nuevas sincronizaciones; operación autenticada en backend que elimine el registro y Auth con reintentos. Evitar recreación por escrituras ya en vuelo. Diseñar el orden, tombstone y reglas antes del despliegue.
**Criterios de aceptación:** fallo o timeout Firestore no deja datos sin limpieza duradera; eliminación repetida es segura; se recupera tras matar la app; limpiar el perfil no recrea el registro; reautenticación fallida no destruye perfil local; el cliente puede consultar estado de la solicitud.
**Pruebas:** emuladores Firebase, timeout, offline, fallo parcial y sync concurrente.
**Estado:** pendiente; no se cambia de forma improvisada el comportamiento de borrado ni se promete supresión completa solo desde el cliente.

### STAB-04 · P1 · Propietario único de flash/micrófono (H02)

**Responsable:** Android. **Dependencias:** STAB-01 y STAB-07. **Estimación:** 2–3 días.

**Alcance:** coordinador de sesiones con propietario/token; prioridad explícita entre Torch, Música y Sonido; cancelar y esperar antes de ceder hardware. Impedir que un `finally` de una sesión vieja apague a la nueva.
**Criterios de aceptación:** una sesión activa de captura como máximo según política; transiciones rápidas y paradas no interrumpen al nuevo propietario; UI sincronizada; errores de cámara observables.
**Pruebas:** controlador falso con secuencia de propietarios, cancelación retardada y pruebas Pixel/Samsung.
**Estado:** entrega 2 implementada en PR #3 y validada en Actions #252 (156 pruebas correctas y Lint correcto); pendiente de QA de dispositivo e integración. `HardwareSessionCoordinator` cancela y espera al propietario anterior (incluidos hijos y limpieza del capturador); cada sesión recibe un TorchController con identidad y los comandos de sesiones antiguas se ignoran. Política: la última sesión que adquiere el coordinador sustituye a la anterior, sin reanudación automática. Los servicios arrancan cada petición con su `startId` y ya no escriben hardware/estado desde `onDestroy`.

Música acumula lecturas no bloqueantes hasta completar un hop; cada pulso espera el cierre del anterior. Sonido procesa detecciones con `collectLatest` dentro de la sesión, espera la terminación del lector antes de cerrar MediaPipe y libera el recorder antes del traspaso. La espera del lector no tiene timeout que permita ceder un micrófono todavía ocupado; un driver que no responda bloqueará el traspaso y debe detectarse en QA físico.

**Cobertura añadida:** ocho pruebas de exclusividad, limpieza retardada, token obsoleto, espera cancelada, veinte peticiones, estado UI, fallo y revocación; una prueba real de espera del executor. **Pendiente:** propagación de errores de Camera2 (el controlador existente aún los encapsula), pruebas Pixel/Samsung y cierre completo de los criterios de STAB-04.

### STAB-05 · P1 · Compras reconocidas y recuperables (H03)

**Responsable:** Android/backend. **Dependencias:** STAB-01. **Estimación:** 1–2 días cliente; backend aparte.

**Alcance entrega 1:** resultado de acknowledge comprobado, tres intentos con espera, no éxito prematuro, cancelación propagada y recuperación al consultar compras.
**Criterios de aceptación:** fallo de acknowledge no concede acceso nuevo; reintento exitoso concede una vez; compra ya reconocida no se reconoce de nuevo; cerrar proceso y volver recupera compra sin reconocer desde Play; UI sale del estado comprando incluso ante excepción.
**Pendiente externo:** validar con cuentas de prueba de Play; verificación servidor/RTDN para operación duradera sin depender de reapertura de app. Los reintentos en memoria no son una cola backend.

### STAB-06 · P1 · Sincronización de suscripciones (H04)

**Responsable:** Android/backend. **Dependencias:** STAB-05. **Estimación:** 1–2 días.

**Alcance entrega 1:** consulta al volver a foreground, refrescos concurrentes acotados y eliminación del retorno que impedía operaciones desconectadas.
**Criterios de aceptación:** compra externa/revocación se refleja al regresar sin matar proceso; recuperar conectividad funciona; error temporal no fuerza una baja falsa solo por una respuesta de red fallida.
**Pendiente:** política offline y estado de antigüedad/autoridad servidor; acordar si el entitlement pertenece a Play o Firebase antes de vincular cuentas.

### STAB-07 · P1 · Autorizar antes de usar hardware (H05)

**Responsable:** Android. **Dependencias:** STAB-01. **Estimación:** 1 día.

**Alcance entrega 1:** sesión de acceso común para servicios y observación de acceso en LED.
**Criterios de aceptación:** entrar en Sonido, dejar caducar Pro y pulsar escuchar no abre micrófono; revocación detiene servicio; LED deja de reproducir al vencer; una sesión sin primera emisión no inicia trabajo.
**Pruebas:** acceso inicial false, true→false, true repetido; repetir en dispositivo con prueba temporal y suscripción.

### STAB-08 · P1 · Captura supervisada (H06)

**Responsable:** Android. **Dependencias:** STAB-01. **Estimación:** 1 día.

**Alcance entrega 1:** dueño explícito de tarea periódica y propagación de RuntimeException de lectura/clasificación; stop voluntario silencia su error esperado; liberar recorder aunque falle stop.
**Criterios de aceptación:** error en segunda ventana produce un único aviso; no continúa ficticiamente escuchando; parar no muestra error; ventana vacía rompe debounce.
**Pruebas:** executor real con error inyectado y parada durante lectura; después smoke de MediaPipe en release con R8.

## Orden de integración

- [x] ~~Entrega 1: Actions correcto, APK probado por el propietario e integración de PR #2.~~
- [x] ~~Entrega 2: implementación, pruebas de transición y build de Actions #252.~~
- [x] ~~Entrega 2: prueba física del propietario en Samsung S26 Ultra e integración de PR #3 (9 de octubre). Captura y LED/pantalla/ambas confirmados.~~
- [ ] Activar STAB-02 con los checks ya publicados.
- [ ] Entrega 3: STAB-03; preparar entorno Firebase y diseñar borrado recuperable antes del despliegue.
- [ ] Completar autoridad backend de compras y validación Play; resolver pendientes P1 y después P2.
- [ ] Candidato release y pista interna: cerrar el checklist físico y de distribución.

## QA de la primera entrega

El artefacto de Actions se llama `lumiai-debug-<número de ejecución>` y contiene el APK. Usa IDs de anuncios de prueba y modo debug; no certifica compras reales ni App Check de release. Tiene el mismo applicationId: si Android rechaza la instalación por firma distinta a una versión release, no desinstalar con datos importantes; usar otro dispositivo/perfil de prueba. Para validar facturación real se necesita un artefacto por la pista interna de Play y cuentas de prueba.

1. Comprobar arranque y versión terminada en `-stabilization.1`.
2. Probar continuo, SOS y cambios de modo como regresión básica.
3. Con acceso Pro, abrir Alerta Sonora, iniciar/parar varias veces y cambiar categorías. Parar no debe mostrar error.
4. Dejar caducar una prueba temporal estando en Sonido sin escuchar y después pulsar Escuchar: debe denegar. Repetir con escucha activa: debe detenerse.
5. Mantener LED reproduciendo al caducar: debe salir del display y permitir volver.
6. Reproducir música en el mismo teléfono y activar Música: no debe pausar el reproductor por solicitar foco.
7. Volver a la app tras cambiar estado de compra en entorno de prueba: comprobar actualización. Usar exclusivamente compras de prueba.
8. Anotar dispositivo, Android, paso, resultado esperado/real y captura si falla. No interpretar un CI verde como validación de estos pasos físicos.


## QA de la segunda entrega

Versión debug: `0.9.54-stabilization.2`. Compilación y pruebas solo en GitHub Actions.

1. Con acceso Pro, iniciar Música, abrir Alerta Sonora y pulsar Escuchar. Música debe apagarse y Sonido debe empezar a escuchar; nunca deben quedar dos capturas activas.
2. Con Sonido escuchando, volver a Linterna y encender Continuo/SOS. Sonido debe dejar de escuchar y la linterna permanecer estable. Repetir Sonido → Música y Música → Sonido.
3. Repetir iniciar/parar y cambiar de modo rápidamente diez veces. No debe haber un apagado tardío, notificación huérfana ni estado encendido sin sesión.
4. En Sonido, cambiar categorías/sensibilidad durante una alerta. Debe detenerse el patrón anterior, reiniciarse el clasificador y seguir llegando la lectura «Oyendo».
5. En Música, reproducir audio en el mismo teléfono: no debe pausar el reproductor; los golpes siguen disparando destellos y Parar libera el indicador de micrófono.
6. Usar el apagado de linterna del sistema: Continuo/Música se paran; Sonido cancela el destello en curso y sigue escuchando.
7. Dejar caducar Pro durante captura. Debe liberarse micrófono/flash y permitir encender inmediatamente un modo gratuito.
8. Repetir con la app en segundo plano y la pantalla bloqueada. Registrar dispositivo, Android y el paso exacto si aparece bloqueo, cierre o interferencia.


## Ampliación del catálogo sonoro · entrega para QA

Versión debug `0.9.54-sound-catalog.1`. Compilación y pruebas exclusivamente en GitHub Actions.

- [x] ~~Cuatro grupos desplegables: puerta/llamadas, alarmas/avisos, personas/mascotas y golpes/roturas.~~
- [x] ~~Cinco sonidos nuevos: maullido, bocina, alarma de coche, marcha atrás y cristal roto. Desactivados por defecto, en pruebas.~~
- [x] ~~Separar llanto general y alarma general de bebé/despertador; etiquetas concretas de sirenas.~~
- [x] ~~Conservar nombres persistidos y ajustes antiguos; añadir patrones y recursos ES/EN.~~
- [ ] CI, instalación y QA física de esta ampliación antes de integrar.
- [ ] Calibración acústica con clips positivos y negativos por categoría. No se afirma precisión medida de las nuevas categorías.
- [ ] Evaluar golpes de puerta vs disparos/petardos; no remapear automáticamente las etiquetas de armas a puerta.
- [ ] Modo exploración en español, historial local sin audio y perfiles en entregas posteriores.

QA propuesta:
1. Actualizar sin borrar datos y comprobar que las ocho categorías previas conservan activación, sensibilidad y canal. Las siete nuevas (incluidas las dos generales) deben estar apagadas.
2. Desplegar cada grupo; abrir/cerrar no cambia ajustes. Activar un sonido nuevo, configurar pantalla/LED/ambas y comprobar persistencia tras cerrar/abrir.
3. Probar cada nuevo sonido por separado y después con ruido de fondo, varios volúmenes y distancias. Anotar aciertos, omisiones y falsas alarmas, sin atribuir porcentaje de precisión a una prueba aislada.
4. Llanto general no debe anunciar bebé; alarma general no debe anunciar despertador. Con la específica activa y puntuación suficiente no debe duplicarse el aviso general.
5. Repetir timbre, puerta, teléfono, perro, bebé, despertador y alarma; comprobar que Parar libera el micrófono y que los tres canales mantienen el comportamiento validado.
