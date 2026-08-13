package com.mejoresiagratis.lumiai.data.system

import android.os.Build

/**
 * Deteccion de fabricante para ajustes especificos de OEM. Unico uso actual (QA
 * 13-ago): en Samsung, el sistema muestra SU PROPIA notificacion de "Linterna
 * encendida / Desactivar" cuando el LED esta en uso — la genera el propio SO,
 * fuera de nuestro paquete, sin API para leerla ni suprimirla. La nuestra es
 * redundante ahi. En el resto de fabricantes (Pixel, etc.) no existe ese
 * equivalente, y la nuestra sigue siendo la unica fuente visible de estado.
 */
object ManufacturerInfo {
    val isSamsung: Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
