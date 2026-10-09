package com.mejoresiagratis.lumiai.domain.sound

/** Las categorías nuevas requieren validación acústica y se muestran como en pruebas. */
enum class SoundReliability { ALTA, MEDIA, EN_PRUEBAS }

/** Agrupación de presentación; no combina puntuaciones ni cambia los canales individuales. */
enum class SoundGroup { PUERTA_LLAMADAS, ALARMAS_AVISOS, PERSONAS_MASCOTAS, GOLPES_ROTURAS }

/** Nombres persistidos estables y etiquetas exactas del modelo YAMNet empaquetado. */
enum class SoundCategory(
    val labels: Set<String>,
    val reliability: SoundReliability,
    val group: SoundGroup,
    val safetyRelated: Boolean = false,
    val transientSound: Boolean = false,
    val enabledByDefault: Boolean = true
) {
    TIMBRE(setOf("Doorbell", "Ding-dong"), SoundReliability.ALTA, SoundGroup.PUERTA_LLAMADAS, transientSound = true),
    GOLPES_PUERTA(setOf("Knock"), SoundReliability.ALTA, SoundGroup.PUERTA_LLAMADAS, transientSound = true),
    TELEFONO(setOf("Telephone", "Telephone bell ringing", "Ringtone"), SoundReliability.ALTA, SoundGroup.PUERTA_LLAMADAS),
    PERRO(setOf("Dog", "Bark"), SoundReliability.ALTA, SoundGroup.PERSONAS_MASCOTAS, transientSound = true),
    BEBE(setOf("Baby cry, infant cry"), SoundReliability.MEDIA, SoundGroup.PERSONAS_MASCOTAS),
    DESPERTADOR(setOf("Alarm clock"), SoundReliability.MEDIA, SoundGroup.ALARMAS_AVISOS),
    SIRENA(setOf("Siren", "Civil defense siren", "Police car (siren)", "Ambulance (siren)", "Fire engine, fire truck (siren)"), SoundReliability.MEDIA, SoundGroup.ALARMAS_AVISOS),
    ALARMA_HUMO(setOf("Smoke detector, smoke alarm", "Fire alarm"), SoundReliability.MEDIA, SoundGroup.ALARMAS_AVISOS, safetyRelated = true),
    GATO(setOf("Meow", "Caterwaul"), SoundReliability.EN_PRUEBAS, SoundGroup.PERSONAS_MASCOTAS, transientSound = true, enabledByDefault = false),
    BOCINA(setOf("Vehicle horn, car horn, honking", "Air horn, truck horn"), SoundReliability.EN_PRUEBAS, SoundGroup.ALARMAS_AVISOS, transientSound = true, enabledByDefault = false),
    ALARMA_COCHE(setOf("Car alarm"), SoundReliability.EN_PRUEBAS, SoundGroup.ALARMAS_AVISOS, enabledByDefault = false),
    MARCHA_ATRAS(setOf("Reversing beeps"), SoundReliability.EN_PRUEBAS, SoundGroup.ALARMAS_AVISOS, enabledByDefault = false),
    CRISTAL_ROTO(setOf("Shatter"), SoundReliability.EN_PRUEBAS, SoundGroup.GOLPES_ROTURAS, transientSound = true, enabledByDefault = false),
    LLANTO_GENERAL(setOf("Crying, sobbing"), SoundReliability.EN_PRUEBAS, SoundGroup.PERSONAS_MASCOTAS, enabledByDefault = false),
    ALARMA_GENERAL(setOf("Alarm", "Buzzer"), SoundReliability.EN_PRUEBAS, SoundGroup.ALARMAS_AVISOS, enabledByDefault = false);

    /** Una alerta específica activa evita un aviso general duplicado del mismo evento. */
    val moreSpecific: Set<SoundCategory>
        get() = when (this) {
            LLANTO_GENERAL -> setOf(BEBE)
            ALARMA_GENERAL -> setOf(DESPERTADOR, SIRENA, ALARMA_HUMO, ALARMA_COCHE, MARCHA_ATRAS)
            else -> emptySet()
        }
}
