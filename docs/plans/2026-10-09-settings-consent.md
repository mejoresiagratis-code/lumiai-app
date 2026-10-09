# Preferencias, consentimiento y cambio de cuenta

Base: main bde8d505db7aae49314d34cd8718a42e61793fc5 (PR #6, validada por el propietario en Samsung S26 Ultra). Firebase permanece en Spark. PR #5 sigue aparcada.

## Hallazgos y cambios

- UMP se consultaba al iniciar AdMob, pero no al cargar ni mostrar anuncios posteriores. Revisar privacidad no invalidaba anuncios existentes ni sus callbacks. Ahora el consentimiento tiene un estado observable y una revisión: al abrir/cerrar el formulario se invalidan anuncios y callbacks anteriores. Cada carga/presentación consulta UMP. No se interpreta canRequestAds como aceptación de publicidad personalizada: UMP decide las solicitudes permitidas.
- Ajustes guardaba con remember el requisito de mostrar las opciones de privacidad y podía perder una actualización asíncrona de UMP. Ahora observa el estado y muestra un mensaje si falla la apertura del formulario.
- El anuncio se consumía solo al cerrar, permitiendo doble pulsación. Ahora se consume antes de presentarlo y cada callback de recompensa se acepta una sola vez, únicamente para su UID original.
- Nombre/país y contador de anuncios usaban claves globales sin propietario. Ahora llevan UID, se ocultan si no coinciden con la cuenta y las escrituras verifican el UID dentro de la transacción DataStore. El desbloqueo temporal permanece en memoria, ligado a UID.
- La combinación de usuario y perfil podía generar una instantánea con nuevo UID y datos antiguos para Firestore. Ahora exige coincidencia de propietario y cancela sincronizaciones obsoletas.
- La limpieza ignoraba errores de disco y cancelación. Ahora intenta limpiar las tres fuentes, propaga cancelación y señala el fallo; el cierre de sesión no continúa si falla la limpieza. El borrado antiguo devuelve error si falla esa limpieza: esto NO sustituye el backend recuperable de PR #5.

## Migración y límites

Los campos antiguos de nombre/país y contador carecen de UID verificable: no se asignan automáticamente a una cuenta. Puede ser necesario volver a introducir nombre/país una vez; el contador antiguo comienza en cero. Al escribir nuevos datos se reemplazan los campos anteriores. No se modifica Firestore, reglas, plan ni facturación.

El modo claro/oscuro, accesibilidad, ajustes de flash, música y alertas son preferencias del dispositivo: sobreviven al cambio de cuenta. Por corrección solicitada en QA, el color de acento y su estilo se reinician a BLUE/VIVID al cerrar sesión y no se heredan al cambiar de cuenta. El estado encendido y el modo activo no persisten; al reiniciar el proceso vuelve apagado y a Continuo. La hora temporal sigue muriendo con el proceso, como ya estaba definido.

Esta entrega no resuelve el borrado remoto pendiente, autoridad de compras en servidor, almacenamiento dañado en general ni sustituye la comprobación real de UMP/AdMob en dispositivo.

## Validación

Solo GitHub Actions: pruebas de aislamiento A/B, escrituras en espera, datos antiguos sin propietario, callbacks tardíos/dobles, consentimiento, cancelación/errores de limpieza y reapertura real de DataStore para preferencias de luz/tema/alertas.

QA dispositivo pendiente:
1. Cambiar tema, intensidad y una alerta a Pantalla/Alta. Cerrar completamente el proceso y abrir: ajustes conservados, flash apagado, modo Continuo.
2. Editar nombre/país en A, cerrar sesión e iniciar B: B no muestra ni hereda los datos ni progreso de A.
3. Abrir opciones de privacidad (cuando UMP lo requiera), modificar y cerrar; comprobar que el botón de anuncios se actualiza y que no se muestra un anuncio antiguo.
4. Dos anuncios de prueba consecutivos: un crédito por anuncio y una hora al completar el segundo. Comprobar doble pulsación y cierre/reapertura de la app.
5. Confirmar que el cierre de sesión conserva preferencias de dispositivo y elimina el desbloqueo temporal.


## Corrección QA de acento (10 de octubre)

El usuario valida persistencia y aislamiento personal, pero informa de naranja heredado tras logout. Era consecuencia de guardar el acento globalmente y restablecer solo colores bloqueados desde SettingsScreen.

- Acento y estilo llevan propietario account:UID o guest; invitado que se registra con el mismo UID es una identidad visual distinta.
- El cierre de sesión restablece color y estilo juntos (azul/vívido), incluidos naranja y azul con estilo cálido; también se aplica al borrado local de cuenta.
- La pérdida de acceso a un color se atiende en ThemeViewModel, también en Inicio. Se verifica la selección dentro de DataStore antes de restablecer para no pisar una elección posterior.
- Selectores y tema usan los mismos permisos efectivos, incluidos overrides God (solo debug). Antes Settings usaba el usuario real mientras otros controles usaban el rol simulado.
- El arranque/reapertura con la misma cuenta conserva su selección; refrescar verificación de correo no reinicia el color.
- Una actualización desde claves sin propietario usa azul/vívido inicialmente, sin afectar otros ajustes.

| Rol efectivo | Azul/naranja | Otros sólidos | Multicolor |
| --- | --- | --- | --- |
| Invitado sin Pro | Sí | No | No |
| Cuenta sin verificar | Sí | Sí | No |
| Cuenta verificada sin Pro | Sí | Sí | No |
| Pro temporal activo | Sí | Sí | Sí |
| Suscripción activa (Play) | Sí | Sí | Sí |
| God en debug | Según permisos simulados | Según permisos simulados | Según permisos simulados |

Cambio de cuenta/logout: azul/vívido en todos los roles. Cambio de permisos dentro de la misma cuenta: restablecer solo si el color deja de estar permitido; naranja permanece si solo caduca Pro. Play y la cuenta Firebase tienen ciclos distintos: una suscripción de Play puede seguir activa como invitado; el reset de identidad sigue aplicándose.

QA pendiente de esta revisión: naranja+cálido -> logout; login con otra cuenta; azul/vívido; mismo usuario reabre y conserva; multicolor pierde Pro desde Inicio; selector de colores con God invitado/cuenta/Pro. La prueba de privacidad/anuncios de la revisión anterior sigue pendiente de confirmación del usuario.
