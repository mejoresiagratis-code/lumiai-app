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
}
