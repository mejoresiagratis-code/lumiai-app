package com.mejoresiagratis.lumiai.domain.sound

/**
 * Serializa [SoundAlertConfig] a una cadena estable (para DataStore) y de vuelta.
 *
 * Formato: una entrada por categoria separada por ';', con campos 'NOMBRE:activado:SENSIBILIDAD'.
 * Ej.: "TIMBRE:1:MEDIA;PERRO:0:ALTA;...". Robusto: las categorias ausentes conservan su valor por
 * defecto y las entradas desconocidas o mal formadas se ignoran (nunca rompe ni inventa).
 */
object SoundAlertConfigCodec {
    private const val ENTRY_SEP = ";"
    private const val FIELD_SEP = ":"

    fun encode(config: SoundAlertConfig): String =
        SoundCategory.entries.joinToString(ENTRY_SEP) { category ->
            val enabled = if (config.isEnabled(category)) "1" else "0"
            category.name + FIELD_SEP + enabled + FIELD_SEP + config.sensitivity(category).name
        }

    fun decode(raw: String?): SoundAlertConfig {
        if (raw.isNullOrBlank()) return SoundAlertConfig()
        val settings = SoundAlertConfig.defaultSettings().toMutableMap()
        raw.split(ENTRY_SEP).forEach { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size != 3) return@forEach
            val category = runCatching { SoundCategory.valueOf(parts[0]) }.getOrNull() ?: return@forEach
            val enabled = parts[1] == "1"
            val sensitivity = runCatching { Sensitivity.valueOf(parts[2]) }.getOrNull()
                ?: settings[category]?.sensitivity ?: Sensitivity.MEDIA
            settings[category] = CategorySetting(enabled, sensitivity)
        }
        return SoundAlertConfig(settings)
    }
}
