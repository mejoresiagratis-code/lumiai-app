package com.mejoresiagratis.lumiai.ui.sound

import androidx.lifecycle.ViewModel
import com.mejoresiagratis.lumiai.domain.sound.Sensitivity
import com.mejoresiagratis.lumiai.domain.sound.SoundAlertConfig
import com.mejoresiagratis.lumiai.domain.sound.SoundCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado del modo Alerta Sonora en memoria. La persistencia (DataStore) y el runtime de IA
 * llegan en fases posteriores; aqui solo se permite probar en dispositivo la divulgacion y la
 * seleccion de las 8 categorias.
 */
@HiltViewModel
class SoundAlertViewModel @Inject constructor() : ViewModel() {

    private val _config = MutableStateFlow(SoundAlertConfig())
    val config: StateFlow<SoundAlertConfig> = _config.asStateFlow()

    fun setEnabled(category: SoundCategory, enabled: Boolean) =
        _config.update { it.withEnabled(category, enabled) }

    fun setSensitivity(category: SoundCategory, sensitivity: Sensitivity) =
        _config.update { it.withSensitivity(category, sensitivity) }

    fun reset() {
        _config.value = SoundAlertConfig()
    }
}
