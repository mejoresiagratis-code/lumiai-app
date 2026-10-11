# STAB-05/06 — validación de compras para la pista de prueba

Actualizado: **11 de octubre de 2026, Europe/Madrid**.
Rama base revisada: `main` en `af5731bde975bdbe1fac9a611a2700d6ad0c691a`.
Estado: **plan de ejecución preparado; pruebas reales de Play pendientes**.

## Decisiones de validación

- **Autoridad de suscripción:** Google Play. La app consulta las suscripciones que posee la cuenta de Play del dispositivo; el UID de Firebase identifica el perfil de LumiAI, pero no concede ni elimina por sí mismo la suscripción. Esto coincide con la arquitectura actual de `DefaultEntitlementRepository` y `PlayBillingRepository`.
- **Sin conexión:** al iniciar un proceso nuevo, LumiAI no concede Pro hasta que Play confirme la compra. Si una suscripción ya se confirmó durante el proceso actual, un fallo temporal de conexión mantiene el acceso en esa sesión; el siguiente resultado correcto de Play lo confirma o lo revoca. El estado no se persiste como una concesión local. Una compra `PENDING` nunca concede Pro.
- **Backend:** no se añade validación de recibos/RTDN en este bloque. El roadmap la aplaza hasta decidir una solución compatible con el proyecto Firebase Spark. Esta prueba valida la integración cliente con Google Play, no certifica una autoridad de servidor.

## Lo que ya hace el cliente

- Producto: `lumiai_pro_monthly`; plan base esperado: `lumiai-pro-mensual`.
- Consulta producto/precio y suscripciones con Play Billing.
- Reconoce compras nuevas antes de activar Pro, con reintentos acotados; intenta reconocer de nuevo al refrescar.
- Restaura la propiedad al conectar y al volver a primer plano.
- Mantiene compras pendientes sin conceder acceso y detiene el acceso cuando una consulta correcta deja de confirmar la suscripción.

**Comprobación previa obligatoria en Play Console:** confirmar que el ID del producto, el plan base y la oferta publicados coinciden exactamente. La app cae a la primera oferta si no encuentra el plan esperado; con más de una oferta, esta salida debe cambiarse para que falle de forma visible antes de activar ofertas adicionales.

## Preparación de Play Console

1. Publicar un AAB de la rama `main` en **Pruebas internas** y habilitar la versión. La versión actual revisada declara `versionCode 83` / `versionName 0.9.54`; cualquier cambio de código para la siguiente entrega debe incrementar el código.
2. Añadir los evaluadores a la lista de testers del canal interno y compartir el enlace de participación. Para el primer lanzamiento, Play puede tardar algunas horas en habilitar el enlace; en posteriores subidas al canal interno suele estar disponible en minutos.
3. En **Configuración > Pruebas de licencia**, añadir las mismas cuentas Google que instalarán desde la pista. Una cuenta necesita estar autorizada como tester de la versión y como tester de licencia para probar compras sin cargo.
4. Comprobar que el producto y el plan base están activos y publicados para pruebas; instalar la app desde Google Play con la cuenta evaluadora correcta. No probar el flujo de facturación con un APK debug instalado lateralmente.
5. No compartir contraseñas ni claves de cuenta en el repositorio. Anotar solo alias de evaluador y resultados.

Google recomienda usar primero la pista interna para la verificación rápida y después ampliar el grupo en prueba cerrada. Referencias oficiales: [configurar pruebas internas/cerradas](https://support.google.com/googleplay/android-developer/answer/9845334?hl=es) y [probar facturación con licencias](https://support.google.com/googleplay/android-developer/answer/6062777?hl=es).

## Matriz de pruebas y aceptación

Marcar resultado, fecha, versión/código y dispositivo. En pruebas que dependan de una cuenta, indicar el alias, nunca el correo completo.

| ID | Escenario | Pasos | Resultado esperado | Estado |
| --- | --- | --- | --- | --- |
| BILL-01 | Producto disponible | Abrir Ajustes > Pro y cargar la ficha desde Play. | Se muestra el producto correcto y el precio localizado; no se usa una oferta distinta del plan declarado. | Pendiente |
| BILL-02 | Cuenta sin compra | Instalar desde el canal interno y abrir LumiAI sin comprar. | La consulta correcta de Play deja Pro desactivado; el hardware Pro permanece bloqueado. | Pendiente |
| BILL-03 | Compra aprobada | Comprar con una cuenta de prueba de licencia. | El callback se procesa; Play reconoce la compra; Pro se activa una sola vez. No hay cargo real. | Pendiente |
| BILL-04 | Cancelación del diálogo | Iniciar compra y cancelar el diálogo de Play. | No se concede Pro ni queda un error falso de compra completada. | Pendiente |
| BILL-05 | Pago pendiente | Usar un método de prueba que produzca estado pendiente, si Play lo ofrece a esa cuenta. | La interfaz informa que está pendiente; no hay Pro hasta que Play notifique/consulte estado `PURCHASED` y se reconozca. | Pendiente |
| BILL-06 | Compra ya existente/restauración | Con una compra activa, reinstalar o borrar datos de la app e iniciar con la misma cuenta de Play. | `queryPurchasesAsync` restaura Pro sin iniciar una segunda compra; se confirma el reconocimiento si faltara. | Pendiente |
| BILL-07 | Reconocimiento con fallo transitorio | Validar mediante prueba automatizada el fallo inicial y éxito del reconocimiento; completar en Play si se puede inducir una desconexión. | Hasta confirmación no se concede Pro por una compra nueva; el refresco reintenta y solo habilita acceso tras confirmar. | Pendiente |
| BILL-08 | Suscripción cancelada, aún vigente | Cancelar renovación desde Google Play mientras el periodo pagado de prueba sigue activo. | Pro sigue disponible hasta que Play deje de devolver la suscripción como comprada/activa. | Pendiente |
| BILL-09 | Caducidad | Usar la renovación acelerada de la suscripción de prueba y esperar su expiración; volver a LumiAI o refrescar. | Una respuesta correcta de Play que ya no incluye la suscripción elimina Pro y corta la sesión de hardware. | Pendiente |
| BILL-10 | Reembolso/revocación | Revocar/reembolsar la compra de prueba desde las herramientas disponibles en Play y refrescar. | Una respuesta correcta de Play que ya no confirma la compra elimina Pro. Registrar demora observada. | Pendiente |
| BILL-11 | Corte de red al arrancar | Desconectar red antes de abrir LumiAI tras forzar el cierre. | Pro permanece desactivado hasta que Play confirme; al restaurar red, refrescar recupera la compra activa. | Pendiente |
| BILL-12 | Corte temporal durante sesión Pro | Confirmar primero la compra; después provocar un fallo temporal de Play/red sin cerrar el proceso. | Se conserva el acceso durante el fallo temporal; al reconectar, Play confirma o revoca. Tras forzar cierre, no se arrastra una concesión local. | Pendiente |
| BILL-13 | Cambio de cuenta LumiAI | Con Pro confirmado en Play, cerrar sesión/cambiar UID Firebase sin cambiar la cuenta de Play del dispositivo. | La compra sigue vinculada a la cuenta Play; el perfil y recompensas siguen aislados por UID. Confirmar que esta separación es comprensible en la UI. | Pendiente |
| BILL-14 | Otra cuenta de Play | Cambiar a una cuenta Play sin suscripción y refrescar. | Si Play confirma que esa cuenta no posee la suscripción, Pro queda desactivado. | Pendiente |

## Criterio para cerrar STAB-05/06

- [ ] Producto y plan base comprobados en Play Console.
- [ ] AAB instalado desde el canal interno, con la firma de Play, en el dispositivo Samsung S26 Ultra.
- [ ] BILL-01 a BILL-14 ejecutadas; casos no reproducibles en pista se sustituyen por una prueba automatizada justificada, sin marcarlos como QA físico.
- [ ] Sin compras completadas sin reconocimiento ni acceso Pro concedido a una compra pendiente/cancelada.
- [ ] Restauración, cancelación vigente, expiración y revocación comprobadas.
- [ ] Política de cuenta Play/Firebase y comportamiento offline aceptados en la UI.
- [ ] GitHub Actions verde para pruebas, Lint y candidato release.
- [ ] Guardar AAB final y mapping R8 fuera de la retención de 7 días de Actions.

La verificación de compras en servidor/RTDN continúa fuera de alcance y debe figurar como riesgo abierto hasta adoptar backend. El canal interno es el paso de validación previo; no equivale a publicar ni a completar la prueba cerrada.
