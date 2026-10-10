# LumiAI — roadmap de estabilización

Actualizado: **10 de octubre de 2026, Europe/Madrid**.
Base de auditoría: `3afc50dc8b41a62f5aa5deaee99a28fcdd710c42`.
Reglas de trabajo: compilaciones y pruebas exclusivamente en GitHub Actions; QA físico por el propietario en Samsung S26 Ultra. Un CI correcto no sustituye pruebas de dispositivo o de Google Play.

Leyenda: [x] y tachado = trabajo completado en el alcance indicado. [ ] = pendiente. Código preparado, integración y QA se distinguen expresamente.

## Integrado en main

### PR #2 — estabilización inicial

- [x] ~~Suite completa de pruebas y Android Lint; release depende de ambos controles.~~
- [x] ~~Reconocimiento de compras con comprobación de resultado, reintentos y recuperación al refrescar.~~
- [x] ~~Refresco de suscripciones al volver a la app y reconexión de Billing.~~
- [x] ~~Comprobación de acceso Pro antes de usar hardware y parada al caducar.~~
- [x] ~~Errores de captura observables, ventanas vacías y música sin interrumpir el reproductor.~~
- [x] ~~QA general del propietario e integración de [PR #2](https://github.com/mejoresiagratis-code/lumiai-app/pull/2).~~

### PR #3 — captura y canales de alerta

- [x] ~~Coordinador exclusivo de flash/micrófono; espera de limpieza y rechazo de órdenes de sesiones antiguas.~~
- [x] ~~Captura PCM16/MIC, ventanas completas y clasificación serializada.~~
- [x] ~~Diagnóstico de audio, vigilancia de ausencia de resultados y notificación con apertura/Parar.~~
- [x] ~~Flash, Pantalla y Ambas respetan la selección; controles de permisos y prueba de pantalla.~~
- [x] ~~QA Samsung S26 Ultra confirmado e integración de [PR #3](https://github.com/mejoresiagratis-code/lumiai-app/pull/3).~~
- [x] ~~[Actions #255](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37956991074): 177 pruebas y Lint correctos.~~

### PR #4 — catálogo de sonidos

- [x] ~~Cuatro grupos desplegables y nuevas categorías: gato, bocina, alarma de coche, marcha atrás y cristal roto.~~
- [x] ~~Llanto general y alarma general separados de bebé/despertador; prioridad de coincidencias específicas.~~
- [x] ~~Persistencia compatible, nombres ES/EN y patrones propios; categorías nuevas inicialmente desactivadas y experimentales.~~
- [x] ~~[Actions #257](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37970309696): 188 pruebas, Lint y APK correctos.~~
- [x] ~~Integración de [PR #4](https://github.com/mejoresiagratis-code/lumiai-app/pull/4), aceptando las limitaciones acústicas documentadas.~~
- [x] ~~QA del propietario: gato, bocina, alarma de coche y cristal roto funcionan; bebé y distinción alarma/sirena también.~~

Limitaciones todavía abiertas:
- [ ] Marcha atrás: el propietario obtiene aviso aumentando sensibilidad; evaluar ajuste de configuración por defecto.
- [ ] Llanto general: el clip de mujer funciona; los ejemplos de hombre probados no. Evaluar muestras positivas y negativas antes de cambiar umbrales o etiquetas.
- [ ] Puerta aporreada: el modelo puede devolver disparos/explosiones. No remapear esas etiquetas automáticamente a puerta.
- [ ] Medir precisión con varios volúmenes, distancias, ruido y dispositivos. Las pruebas puntuales no certifican una tasa de acierto.
- [ ] Futuro opcional, no comprometido: exploración en español, historial local sin audio y perfiles.

### PR #6 — recuperación de cámara/flash

- [x] ~~Errores de cámara tipados y mensajes ES/EN; fin de los fallos de encendido ocultos.~~
- [x] ~~Reintento al volver a encender sin reiniciar LumiAI; pérdida de disponibilidad atendida y comandos/callbacks serializados.~~
- [x] ~~Las alertas sonoras mantienen escucha y pantalla cuando falla el LED; siguiente detección puede recuperar flash.~~
- [x] ~~[Actions #264](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37994067218): 198 pruebas, Lint y APK correctos.~~
- [x] ~~QA Samsung aprobado por el propietario: «Pruebas ok todo funciona». [PR #6](https://github.com/mejoresiagratis-code/lumiai-app/pull/6) integrada.~~
- [ ] Ampliar QA a Pixel/otros fabricantes; no generalizar la validación de Samsung.

### Conservación de artefactos

- [x] ~~El propietario cambia la conservación a 7 días; verificado en main `5a2315f1e621c7b64e9297d2f6f1dbbce93034ee`.~~
- [x] ~~APK debug, pruebas, Lint, AAB/APK release y mapping R8 configurados con `retention-days: 7`.~~
- [ ] Limpieza manual de artefactos antiguos si se desea liberar espacio inmediato; no consta completada.
- [ ] Para futuras publicaciones, conservar el candidato distribuido y su mapping R8 más allá de la caducidad del artefacto de Actions.

### PR #7 — preferencias por cuenta y consentimiento

[PR #7](https://github.com/mejoresiagratis-code/lumiai-app/pull/7) **integrada en main**, commit `1027e1f3d0195a5dd9fd1dc84eaaf352d6941c40`.
APK validada: **#269**, versión `0.9.54-settings-consent.3`.
Commit probado: `a904de84861059ab3e9fc34d292df8ee6fdc1079`.
QA general Samsung aceptado por el propietario el 10 de octubre: «Pr7 funcionando ok». No implica certificación exhaustiva de cada escenario de Play/UMP.

- [x] ~~Perfil personal, progreso de anuncios y desbloqueo temporal vinculados al UID.~~
- [x] ~~Bloqueo de instantáneas usuario/perfil incoherentes y escrituras tardías de otra cuenta.~~
- [x] ~~Consentimiento observable; invalidación de anuncios/callbacks antiguos, comprobación antes de cargar/mostrar y errores de formulario visibles.~~
- [x] ~~Consumo del anuncio antes de presentarlo y recompensa única para su cuenta original.~~
- [x] ~~Errores de limpieza visibles, cancelación propagada y cierre de sesión detenido ante fallo local.~~
- [x] ~~Pruebas de reapertura de DataStore y conservación de preferencias del dispositivo.~~
- [x] ~~QA parcial del propietario sobre #266: persistencia funciona y cambio de cuenta funciona excepto el acento naranja heredado.~~
- [x] ~~Corregido en #267: cerrar sesión/cambiar identidad vuelve a azul y vívido, incluido naranja; tema separado en la revisión #269.~~
- [x] ~~Control de colores bloqueados fuera de Ajustes y permisos del selector coherentes con God.~~
- [x] ~~[Actions #267](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37998244865): 220 pruebas, cero fallos/errores/omitidas; Lint y APK correctos.~~
- [x] ~~Tema Claro/Oscuro/Sistema persistido por cuenta en el dispositivo; salir restablece Sistema para invitado, volver recupera su elección.~~
- [x] ~~[Actions #269](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/38001415528): 228 pruebas, cero fallos/errores/omitidas; Lint y APK correctos.~~
- [x] ~~QA general del propietario e integración de PR #7.~~
- [ ] Ampliar pruebas de producción de UMP/AdMob y matriz de roles/compras reales en pista interna.

Matriz de acentos:
| Rol efectivo | Azul/naranja | Otros sólidos | Multicolor |
| --- | --- | --- | --- |
| Invitado sin Pro | Sí | No | No |
| Cuenta sin verificar | Sí | Sí | No |
| Cuenta verificada sin Pro | Sí | Sí | No |
| Pro temporal activo | Sí | Sí | Sí |
| Suscripción Play activa | Sí | Sí | Sí |
| God (debug) | Según permisos simulados | Según permisos simulados | Según permisos simulados |

Cerrar sesión/cambiar cuenta restablece azul/vívido independientemente del color permitido. Cambiar solo permisos dentro de la misma cuenta restablece únicamente el color que pierde acceso. La suscripción Play y la identidad Firebase son estados distintos.

Migración: nombre/país y contador antiguos sin propietario no se asignan por suposición; pueden requerir reentrada de datos y contador a cero. Acentos antiguos sin propietario parten de azul/vívido. El tema antiguo global sin propietario vuelve a Sistema; las nuevas elecciones se guardan por UID localmente, sin sincronización entre dispositivos. Accesibilidad y ajustes de luz/sonidos permanecen como preferencias del dispositivo.

### PR #8 — selector de idioma previo al acceso

[PR #8](https://github.com/mejoresiagratis-code/lumiai-app/pull/8) **integrada en main**, squash commit `ebc61d3c59c311494c42a1c8b3fc35dd6b554cc5`.

- [x] ~~Selector de idioma accesible desde la bienvenida y la pantalla de acceso, antes de iniciar sesión o crear una cuenta.~~
- [x] ~~En Android 13+, enlace al selector de idioma por aplicación; en versiones anteriores, LumiAI sigue el idioma del sistema.~~
- [x] ~~[Actions #272](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/38020366567): pruebas unitarias, Android Lint y APK debug correctos.~~
- [x] ~~QA físico confirmado por el propietario: «Probado y funcionando» (10 de octubre de 2026).~~

## Aparcado por decisión del propietario — PR #5 / STAB-03

Proyecto Firebase **`lumiai-37ab0`**, proyecto actual de la app, plan **Spark**. El propietario decide mantener Spark.

- [x] ~~Código de borrado recuperable preparado en [PR #5](https://github.com/mejoresiagratis-code/lumiai-app/pull/5): recibo persistente, backend idempotente, reintentos y barrera de sincronización.~~
- [x] ~~[Actions #260](https://github.com/mejoresiagratis-code/lumiai-app/actions/runs/37980066426): 196 pruebas Android, 9 pruebas Firebase/reglas, Lint y APK correctos.~~
- [ ] **Aparcado:** despliegue de Functions/Scheduler que requiere facturación habilitada, App Check real y QA.
- [ ] **No integrado ni desplegado.** No usar su APK como actualización de la rama mantenida en Spark.
- [ ] Retomar con backend autorizado o diseñar una alternativa compatible con Spark. El borrado remoto completo/recuperable sigue sin resolverse en main.

## Siguiente orden de trabajo

1. **Compras y recompensas — STAB-05/06 (P1):** cuentas de prueba y pista interna Play; compra, restauración, reconocimiento, caducidad, revocación y pérdida de conexión. Definir vinculación Play/Firebase y política offline.
2. **Protección de main — STAB-02 (P1):** administrador configura PR y checks obligatorios; el YAML por sí solo no protege la rama. No consta aplicado.
3. **Pendientes de sonido:** sensibilidad de marcha atrás, llanto general y golpes de puerta; conjunto reproducible de muestras y control de falsos positivos.
4. **P2 restantes:** reloj/caducidad ante cambios de hora, recuperación de almacenamiento dañado, LED, dependencias, reglas y permisos de CI/cadena de suministro. Desglosar por riesgo antes de implementar.
5. **Candidato release:** QA físico MediaPipe/R8, compatibilidad 16 KB, batería, segundo plano, bloqueo y otros fabricantes; firma y archivo de mapping/candidato.
6. **Publicación:** pista interna de Play y comprobaciones finales; publicación requiere autorización explícita.

Verificación de compras en servidor/RTDN queda aplazada mientras no haya una solución de backend elegida. No declarar cerrado un P1 por haber integrado solo su parte cliente.

## Cobertura de los P1 originales

| ID | Estado |
| --- | --- |
| STAB-01 — CI | Integrado; pruebas y Lint bloquean el job release. |
| STAB-02 — Protección de main | Pendiente de administración y prueba efectiva del bloqueo. |
| STAB-03 — Borrado recuperable | PR #5 preparada; aparcada, sin despliegue ni integración. |
| STAB-04 — Exclusividad de hardware | PR #3/#6 integradas y Samsung aprobado; QA de otros fabricantes pendiente. |
| STAB-05 — Compras reconocidas | Cliente mejorado en PR #2; validación real Play y backend pendientes. |
| STAB-06 — Suscripciones | Refresco/reconexión integrados; política offline y autoridad/vinculación pendientes. |
| STAB-07 — Acceso al hardware | Integrado; ampliar pruebas con compras reales y caducidad en dispositivos. |
| STAB-08 — Captura supervisada | Integrado; falta completar candidato release/MediaPipe y QA ampliado. |

Historial técnico de detalle: [cámara/flash](2026-10-09-hardware-recovery.md) y [preferencias/consentimiento](https://github.com/mejoresiagratis-code/lumiai-app/blob/codex/settings-consent-01/docs/plans/2026-10-09-settings-consent.md). Los detalles del backend aparcado permanecen en la rama de PR #5.
