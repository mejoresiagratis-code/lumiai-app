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
    val safetyRelated: Boolean = false,
    /**
     * Sonido TRANSITORIO (QA 14-ago): dura menos que las ~2 ventanas de clasificacion
     * (~1 s) que exige el debounce estandar — un ladrido o un golpe en la puerta cruzan
     * UNA ventana y morian en el debounce sin disparar jamas. Para estos, el motor exige
     * solo 1 ventana; el cooldown sigue conteniendo el spam.
     */
    val transientSound: Boolean = false
) {
    TIMBRE(setOf("Doorbell", "Ding-dong"), SoundReliability.ALTA, transientSound = true),
    GOLPES_PUERTA(setOf("Knock"), SoundReliability.ALTA, transientSound = true),
    TELEFONO(setOf("Telephone", "Telephone bell ringing", "Ringtone"), SoundReliability.ALTA),
    PERRO(setOf("Dog", "Bark"), SoundReliability.ALTA, transientSound = true),
    BEBE(setOf("Baby cry, infant cry", "Crying, sobbing"), SoundReliability.MEDIA),
    DESPERTADOR(setOf("Alarm clock", "Alarm"), SoundReliability.MEDIA),
    SIRENA(setOf("Siren", "Civil defense siren", "Emergency vehicle"), SoundReliability.MEDIA),
    ALARMA_HUMO(
        setOf("Smoke detector, smoke alarm", "Fire alarm"),
        SoundReliability.MEDIA,
        safetyRelated = true
    );
}
