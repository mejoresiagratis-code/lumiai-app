package com.mejoresiagratis.lumiai.domain.sound

/**
 * Fiabilidad esperada de deteccion. v1 no incluye ninguna categoria de fiabilidad baja
 * (honestidad: no ofrecer deteccion que falle demasiado a menudo).
 */
enum class SoundReliability { ALTA, MEDIA }

/**
 * Categorias de sonido vigiladas por el modo Alerta Sonora. v1 incluye las 8 mas usadas en
 * apps de alerta sonora / accesibilidad auditiva.
 *
 * [labels] son los nombres de clase de AudioSet/YAMNet que componen cada categoria; alimentan
 * la allowlist del clasificador en F2.
 *
 * IMPORTANTE: estos nombres provienen de la ontologia AudioSet y DEBEN validarse caracter a
 * caracter contra el mapa de etiquetas del modelo .tflite empaquetado antes de activar el
 * runtime de IA. Si un nombre no coincide exactamente, esa clase nunca disparara.
 */
enum class SoundCategory(
    val labels: Set<String>,
    val reliability: SoundReliability,
    val safetyRelated: Boolean = false
) {
    TIMBRE(setOf("Doorbell", "Ding-dong"), SoundReliability.ALTA),
    GOLPES_PUERTA(setOf("Knock"), SoundReliability.ALTA),
    TELEFONO(setOf("Telephone", "Telephone bell ringing", "Ringtone"), SoundReliability.ALTA),
    PERRO(setOf("Dog", "Bark"), SoundReliability.ALTA),
    BEBE(setOf("Baby cry, infant cry", "Crying, sobbing"), SoundReliability.MEDIA),
    DESPERTADOR(setOf("Alarm clock", "Alarm"), SoundReliability.MEDIA),
    SIRENA(setOf("Siren", "Civil defense siren", "Emergency vehicle"), SoundReliability.MEDIA),
    ALARMA_HUMO(
        setOf("Smoke detector, smoke alarm", "Fire alarm"),
        SoundReliability.MEDIA,
        safetyRelated = true
    );
}
