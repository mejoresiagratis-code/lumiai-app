package com.mejoresiagratis.lumiai.util

import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.domain.model.FlashSettings
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFlashStateRepository : FlashStateRepository {
    private val _isOn = MutableStateFlow(false)
    private val _mode = MutableStateFlow(FlashMode.CONTINUOUS)
    private val _settings = MutableStateFlow(FlashSettings())
    override val isOn = _isOn.asStateFlow()
    override val mode = _mode.asStateFlow()
    override val settings = _settings.asStateFlow()
    override fun setOn(on: Boolean) { _isOn.value = on }
    override suspend fun setMode(mode: FlashMode) { _mode.value = mode }
    override suspend fun updateSettings(transform: (FlashSettings) -> FlashSettings) {
        _settings.value = transform(_settings.value).coerced()
    }
}
