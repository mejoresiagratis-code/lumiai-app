package com.mejoresiagratis.lumiai.data.sound

import androidx.annotation.StringRes
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory

/** Nombre visible de cada categoria como recurso (compartido por la pantalla y el servicio). */
@StringRes
fun SoundCategory.labelRes(): Int = when (this) {
    SoundCategory.TIMBRE -> R.string.sound_cat_doorbell
    SoundCategory.GOLPES_PUERTA -> R.string.sound_cat_knock
    SoundCategory.TELEFONO -> R.string.sound_cat_phone
    SoundCategory.PERRO -> R.string.sound_cat_dog
    SoundCategory.BEBE -> R.string.sound_cat_baby
    SoundCategory.DESPERTADOR -> R.string.sound_cat_alarm_clock
    SoundCategory.SIRENA -> R.string.sound_cat_siren
    SoundCategory.ALARMA_HUMO -> R.string.sound_cat_smoke
}
