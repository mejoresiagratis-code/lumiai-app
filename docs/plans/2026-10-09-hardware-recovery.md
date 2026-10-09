# Recuperación de cámara y flash

Base: main 493af8868b8750766018f3a85498dae4c5ea12c5. Esta rama no incluye el backend Firebase de PR #5; el proyecto continúa en Spark.

## Problemas corregidos

- Camera2TorchController ocultaba excepciones de encendido y la UI podía seguir encendida sin LED.
- La búsqueda lazy guardaba null permanentemente tras un fallo transitorio de CameraManager.
- No se observaba onTorchModeUnavailable durante una sesión activa.
- Un fallo de LED no tenía un mensaje persistente distinguible de un fallo del clasificador.

## Comportamiento

- Razones tipadas: cámara ocupada, permiso denegado, bloqueo de Android, indisponibilidad, falta de flash y fallo al apagar. Mensajes ES/EN visibles en el panel de la linterna y música.
- Los fallos de encendido cancelan el patrón y liberan la sesión; el usuario puede cerrar la cámara y volver a encender sin reiniciar LumiAI. No se enciende solo cuando otra app libera la cámara.
- Las alertas sonoras mantienen la captura; indican fallo de entrega y reintentan el LED en la próxima detección que corresponda. Pantalla y Ambas conservan el comportamiento de sus canales.
- La limpieza no lanza excepciones que oculten el error inicial. El apagado fallido conserva la referencia para que el finalizador pueda reintentarlo e indica comprobar el control de Android.
- La capacidad física usa FEATURE_CAMERA_FLASH; la búsqueda de cámara se puede repetir tras un fallo. Los cambios de disponibilidad estando inactivo no generan errores falsos.

## Validación

Pruebas y compilación exclusivamente en GitHub Actions. Se añaden pruebas Robolectric del controlador para cámara ocupada, recuperación de descubrimiento, permisos, bloqueo, disponibilidad asíncrona y errores de apagado. La comprobación real de Camera2/OEM requiere dispositivo.

QA Samsung S26 Ultra pendiente:
1. Linterna continua, SOS y estrobo: encender/apagar; comprobar que no aparecen errores por sus propios pulsos.
2. Abrir Cámara mientras se usa el LED: LumiAI debe parar, reflejar estado apagado y explicar la indisponibilidad. Cerrar Cámara y volver a encender.
3. Repetir con música y alternar modos rápidamente: sin dos propietarios del LED ni reencendidos tardíos.
4. Alertas sonoras con LED ocupado: escucha activa y aviso de fallo; con Ambas verificar pantalla. Tras liberar Cámara, una nueva detección debe recuperar flash.
5. Solo Pantalla: no intentar encender LED ni detener escucha por cámara ocupada.
6. Usar Desactivar de la notificación de linterna de Samsung: no reencender automáticamente.

PR #5 de borrado recuperable permanece aparcada: no fusionar ni distribuir su APK como actualización de esta rama mientras se mantenga Spark.
