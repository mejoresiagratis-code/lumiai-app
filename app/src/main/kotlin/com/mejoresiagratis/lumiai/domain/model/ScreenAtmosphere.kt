package com.mejoresiagratis.lumiai.domain.model

/**
 * Atmosferas (degradados) del Modo Intimo, extension del Modo Pantalla. [top]/[bottom]
 * son ARGB para un Brush.verticalGradient; no requieren mezcla de color en tiempo real.
 */
enum class ScreenAtmosphere(val top: Int, val bottom: Int) {
    FUEGO(top = 0xFF3D0A00.toInt(), bottom = 0xFFFF6A00.toInt()),
    ATARDECER(top = 0xFF4A0033.toInt(), bottom = 0xFFFF8A3D.toInt()),
    VELA(top = 0xFFFFB300.toInt(), bottom = 0xFFFFE082.toInt()),
    NEBULOSA(top = 0xFF120033.toInt(), bottom = 0xFF0B1E4A.toInt())
}

/**
 * Animacion del Modo Intimo.
 * - [ESTATICO]: degradado fijo.
 * - [RESPIRACION]: la luminosidad global fluctua en un ciclo lento (4-6 s).
 * - [LLAMA]: parpadeo pseudoaleatorio rapido de opacidad (simula una vela).
 */
enum class ScreenAnimation { ESTATICO, RESPIRACION, LLAMA }
