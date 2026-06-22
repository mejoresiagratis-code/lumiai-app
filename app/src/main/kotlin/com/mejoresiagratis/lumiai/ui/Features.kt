package com.mejoresiagratis.lumiai.ui

/**
 * Flags de UI. Permiten activar pantallas nuevas sin borrar las antiguas
 * (revertir = poner el flag a false). Migración Beam Hub, Fase 2.
 */
object Features {
    /** Usa la pantalla Beam Hub (orbe central + fondo por modo) en lugar del grid clásico. */
    const val BEAM_HUB: Boolean = true
}
