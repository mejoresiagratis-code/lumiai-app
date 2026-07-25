package com.mejoresiagratis.lumiai.ui.led

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.model.LedBannerConfig
import com.mejoresiagratis.lumiai.domain.repository.LedBannerConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedBannerViewModel @Inject constructor(
    private val repo: LedBannerConfigRepository
) : ViewModel() {

    val config: StateFlow<LedBannerConfig> = repo.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LedBannerConfig())

    fun update(transform: (LedBannerConfig) -> LedBannerConfig) {
        viewModelScope.launch { repo.setConfig(transform(config.value)) }
    }
}
