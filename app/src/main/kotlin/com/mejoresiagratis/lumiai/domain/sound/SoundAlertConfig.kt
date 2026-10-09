package com.mejoresiagratis.lumiai.domain.sound

/**
 * Sensibilidad de deteccion por categoria. Mas sensibilidad = umbral de confianza mas bajo:
 * salta con menos certeza (mas avisos, mas falsos positivos). Menos sensibilidad = umbral alto:
 * solo avisa cuando esta muy seguro (menos falsos positivos, mas riesgo de perder un sonido).
 */
enum class Sensitivity(val scoreThreshold: Float) {
    BAJA(0.7f),
    MEDIA(0.5f),
    ALTA(0.3f)
}

/**
 * Cómo avisa una categoría cuando se detecta. El LED es monocolor (honestidad): las categorías se
 * distinguen por ritmo, no por color. [usesFlash] requiere que el dispositivo tenga flash; en su
 * ausencia se informa al usuario. No se sustituye un canal por otro sin elegirlo.
 */
enum class AlertChannel(val usesFlash: Boolean, val usesScreen: Boolean) {
    FLASH(usesFlash = true, usesScreen = false),
    PANTALLA(usesFlash = false, usesScreen = true),
    AMBAS(usesFlash = true, usesScreen = true)
}

/** Ajuste de una categoria: si esta vigilada, con que sensibilidad y como avisa. */
data class CategorySetting(
    val enabled: Boolean,
    val sensitivity: Sensitivity = Sensitivity.MEDIA,
    val channel: AlertChannel = AlertChannel.FLASH
)

/**
 * Configuracion del modo Alerta Sonora. Modelo puro e inmutable, persistible en DataStore (F3).
 * Las ocho categorías originales vienen activadas; las nuevas requieren activación explícita.
 * Las de seguridad arrancan con sensibilidad alta para
 * no perderlas.
 */
data class SoundAlertConfig(
    val settings: Map<SoundCategory, CategorySetting> = defaultSettings()
) {
    fun isEnabled(category: SoundCategory): Boolean = settings[category]?.enabled == true

    fun sensitivity(category: SoundCategory): Sensitivity =
        settings[category]?.sensitivity ?: Sensitivity.MEDIA

    fun channel(category: SoundCategory): AlertChannel =
        settings[category]?.channel ?: AlertChannel.FLASH

    /** Umbral de confianza efectivo para una categoria. */
    fun threshold(category: SoundCategory): Float = sensitivity(category).scoreThreshold

    /**
     * Union de etiquetas AudioSet de las categorias activas. Si está vacía, no hay nada que vigilar. El clasificador devuelve todas las etiquetas
     * para diagnóstico; el motor filtra las activas.
     */
    fun activeLabels(): Set<String> =
        SoundCategory.entries.filter { isEnabled(it) }.flatMap { it.labels }.toSet()

    /** Si hay al menos una categoria activa. */
    val anyEnabled: Boolean get() = settings.any { it.value.enabled }

    fun withEnabled(category: SoundCategory, enabled: Boolean): SoundAlertConfig {
        val current = settings[category] ?: CategorySetting(enabled = false)
        return copy(settings = settings + (category to current.copy(enabled = enabled)))
    }

    fun withSensitivity(category: SoundCategory, sensitivity: Sensitivity): SoundAlertConfig {
        val current = settings[category] ?: CategorySetting(enabled = true)
        return copy(settings = settings + (category to current.copy(sensitivity = sensitivity)))
    }

    fun withChannel(category: SoundCategory, channel: AlertChannel): SoundAlertConfig {
        val current = settings[category] ?: CategorySetting(enabled = true)
        return copy(settings = settings + (category to current.copy(channel = channel)))
    }

    companion object {
        fun defaultSettings(): Map<SoundCategory, CategorySetting> =
            SoundCategory.entries.associateWith { category ->
                val sensitivity = if (category.safetyRelated) Sensitivity.ALTA else Sensitivity.MEDIA
                // Las de seguridad avisan por flash y pantalla por defecto (más difícil de perder).
                val channel = if (category.safetyRelated) AlertChannel.AMBAS else AlertChannel.FLASH
                CategorySetting(enabled = category.enabledByDefault, sensitivity = sensitivity, channel = channel)
            }
    }
}
