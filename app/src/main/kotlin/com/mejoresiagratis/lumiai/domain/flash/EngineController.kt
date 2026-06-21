package com.mejoresiagratis.lumiai.domain.flash

/** Arranca/para el motor (en Fase 1, via foreground service). */
interface EngineController {
    fun start()
    fun stop()
}
