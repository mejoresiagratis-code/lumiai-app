# Borrado recuperable de cuenta

## Estado de entrega

Implementación preparada para CI y un proyecto Firebase de pruebas. **No desplegada en producción.** La app requiere las nuevas funciones y reglas; si faltan, conserva la cuenta, los datos y una solicitud local pendiente. No vuelve al borrado directo antiguo. No distribuir esta variante a usuarios antes de desplegar y validar el backend.

## Contrato

1. Android persiste UID y recibo aleatorio (dos UUID) antes de solicitar el borrado. Suspende su sincronización de ese UID.
2. `deleteMyAccount`, en `europe-west1`, exige Auth no anónimo, App Check válido, UID esperado igual al autenticado y autenticación de los últimos cinco minutos para una solicitud nueva. Reautenticación existente por contraseña/Google sigue disponible.
3. Una transacción crea `accountDeletions/{uid}` con estado pending y hash SHA256 del recibo. Solo Admin puede escribir estos registros. Las reglas bloquean cualquier escritura de cliente a `users/{uid}` mientras exista el marcador, incluyendo clientes anteriores y otros dispositivos.
4. El servidor elimina recursivamente `users/{uid}` (incluidas subcolecciones), después Auth, y solo entonces marca completed. Un fallo conserva pending. Auth inexistente se considera idempotente.
5. `retryAccountDeletions` reintenta cada cinco minutos hasta 50 solicitudes, ordenando por próximo intento para que un fallo no bloquee el resto. No depende de que la app siga abierta. Vigilar errores y backlog; no hay límite artificial que abandone un borrado.
6. `accountDeletionStatus` exige App Check y posesión del recibo aleatorio; permite confirmar el resultado tras perder Auth. No crea solicitudes, no acepta órdenes de borrado y no revela perfiles. El recibo nunca se registra ni almacena en claro en el servidor.
7. Android persiste la confirmación antes de limpiar datos locales y cerrar sesión. Si falla la limpieza local, la reintenta al volver a Ajustes. Nunca limpia una cuenta distinta. Un timeout significa pendiente, no «cuenta borrada».
8. El marcador mínimo (UID como clave, hash y fechas/estado) se purga siete días después de completar, mediante el mismo planificador. Los pendientes no caducan. La ventana de consulta del recibo es de siete días; después puede requerirse verificar el caso desde soporte. No recrear manualmente un UID eliminado.

El alcance del borrado es Auth, `users/{uid}` y datos locales de perfil, progreso de anuncios y desbloqueo temporal. No cancela suscripciones de Google Play ni elimina registros independientes de Play/Crashlytics/servicios de terceros. Si se añaden nuevos almacenes de datos de usuario, se deben incorporar al proceso antes de considerarlo completo.

## Despliegue y entorno de prueba

Se necesita acceso al proyecto Firebase y permisos para desplegar reglas, índices y Cloud Functions/Cloud Scheduler, además de facturación habilitada según requisitos de Firebase. No hay credenciales de despliegue en este repositorio ni se solicitan claves privadas por chat.

1. Usar un proyecto de pruebas separado y configurar la app con su `google-services.json`. No usar cuentas reales para QA.
2. Registrar el App Check debug token del dispositivo de prueba; verificar Play Integrity para release.
3. Con Firebase CLI autenticada, desplegar primero el índice y las reglas:
   `firebase deploy --project <PROYECTO_PRUEBAS> --only firestore:rules,firestore:indexes`
4. Esperar a que el índice de accountDeletions esté listo. Desplegar el codebase:
   `firebase deploy --project <PROYECTO_PRUEBAS> --only functions:account-deletion`
5. Verificar región europe-west1, Scheduler activo, permisos de la cuenta de servicio para Auth/Firestore/App Check y alertas de errores. Las funciones administrativas requieren privilegios efectivos; un despliegue aceptado no demuestra que puedan borrar.
6. Ejecutar QA Android con cuenta desechable: borrar, reautenticar, perder red, cerrar app, reabrir Ajustes, y comprobar que el perfil no reaparece. Verificar en consola que desaparecen Auth y el documento/subcolecciones; el marcador pasa a completed.
7. Probar segundo dispositivo/cliente antiguo escribiendo durante y después del borrado: Firestore debe rechazarlo. Probar cambio de cuenta durante solicitud: no se limpia la nueva.
8. Solo después, desplegar en producción y publicar la app. No revertir reglas para permitir escrituras a cuentas en borrado; pausar distribución si hay un fallo de despliegue.

## CI

GitHub Actions ejecuta Android y un job separado con emuladores Auth/Firestore sobre `demo-lumiai-deletion`. No accede a datos reales. Verifica reglas de aislamiento, no recreación, reintentos, subcolecciones, orden de eliminación, idempotencia y recibos. App Check real, transporte HTTP/callable y Scheduler desplegado requieren QA del entorno de pruebas; los emuladores no los certifican.

Referencias: https://firebase.google.com/docs/functions/callable · https://firebase.google.com/docs/app-check/cloud-functions · https://firebase.google.com/docs/firestore/manage-data/delete-data · https://firebase.google.com/docs/functions/schedule-functions
