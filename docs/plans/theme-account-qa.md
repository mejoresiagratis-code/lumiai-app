# Tema por cuenta — PR7

Claro/Oscuro/Sistema se guarda localmente por UID, independiente del rol. Se conserva al reabrir y al volver a esa cuenta. No se sincroniza entre dispositivos.

Al cerrar sesión se restablece Sistema para el invitado. Una cuenta sin preferencia comienza en Sistema. No se asigna la antigua preferencia global a ninguna cuenta porque no se puede determinar su propietario. El acento mantiene las reglas de la revisión anterior.

## Validación manual pendiente

- Sistema claro: cuenta A elige Oscuro; cerrar y reabrir conserva Oscuro.
- Cerrar sesión: invitado vuelve a Sistema (claro).
- Cuenta B nueva: Sistema; elegir Claro.
- Volver a A: Oscuro; volver a B: Claro.
- Elegir Sistema y cambiar el tema del teléfono: la app lo sigue.
- Repetir con cuenta gratuita, Pro temporal y Pro: el tema no depende del rol.

Compilación y pruebas exclusivamente en GitHub Actions.
