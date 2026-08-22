package com.mejoresiagratis.lumiai.data.system

/**
 * IDs de notificación de TODA la app, en un único sitio (17-ago).
 *
 * Los IDs son globales al paquete, no por servicio: dos servicios distintos que usen el mismo
 * número se pisan entre sí. Ya ocurrió dos veces —la detección de sonido machacó primero la
 * notificación del propio servicio de Alerta (ID 2, corregido el 14-ago) y después la del
 * servicio de Música (ID 4, corregido hoy)— porque cada servicio elegía su número mirando solo
 * sus propias constantes.
 *
 * Regla: cualquier notificación nueva se declara AQUÍ, nunca como constante local.
 */
object NotificationIds {
    /** Servicio de linterna en primer plano. */
    const val TORCH = 1

    /** Servicio de Alerta sonora en primer plano ("Escuchando..."). */
    const val SOUND_ALERT_FOREGROUND = 2

    /** Aviso a pantalla completa de la Alerta sonora. */
    const val SOUND_ALERT_SCREEN = 3

    /** Servicio de modo Música en primer plano. */
    const val MUSIC_FOREGROUND = 4

    /** Evento puntual: sonido reconocido por la Alerta sonora. */
    const val SOUND_ALERT_DETECTION = 5
}
