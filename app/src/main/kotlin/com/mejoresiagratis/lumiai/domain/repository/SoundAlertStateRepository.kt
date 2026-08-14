package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Refleja si el SERVICIO de Alerta Sonora esta realmente vivo — no si el usuario tocó el
 * botón. Antes de esto, la pantalla llevaba un `listening` local optimista que se ponía a
 * `true` en el tap sin comprobar nunca que el servicio arrancara de verdad: si moría por
 * cualquier razón (incluido un fallo interno), el botón se quedaba mintiendo "Parar" para
 * siempre (QA 13-ago).
 */
interface SoundAlertStateRepository {
    val listening: StateFlow<Boolean>
    fun setListening(value: Boolean)

    /**
     * Motivo de la ULTIMA parada inesperada del servicio, o null si no la hubo (o el
     * servicio arranco bien despues). La pantalla lo muestra: convierte cada fallo en
     * un diagnostico legible en el propio movil en vez de una adivinanza a ciegas
     * (QA 14-ago — "el boton revierte al instante y nunca se sabe por que").
     */
    val stopReason: StateFlow<String?>
    fun setStopReason(value: String?)

    /**
     * Lo que el clasificador esta oyendo AHORA (top de scores de la ultima ventana) y la
     * ultima alerta disparada. Observabilidad en el movil (QA 14-ago): "no detecta nada"
     * tiene dos causas posibles indistinguibles a ciegas — el clasificador no oye, o los
     * umbrales no dejan pasar. Con los scores en vivo en pantalla, una prueba lo decide.
     */
    val lastWindow: StateFlow<String?>
    fun setLastWindow(value: String?)
    val lastDetection: StateFlow<String?>
    fun setLastDetection(value: String?)
}
