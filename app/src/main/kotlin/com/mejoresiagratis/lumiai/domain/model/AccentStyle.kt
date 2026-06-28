package com.mejoresiagratis.lumiai.domain.model

/**
 * Estilo de render del color de acento, elegible en Ajustes tras el tema y el acento.
 * - [WARM]: tono cálido y sobrio (paleta TonalSpot). Da un oro fiel al amarillo de marca.
 * - [VIVID]: tono saturado y directo (paleta Content), conserva el primary ≈ semilla.
 * No afecta al acento Blanco (siempre monocromo).
 */
enum class AccentStyle { WARM, VIVID }
