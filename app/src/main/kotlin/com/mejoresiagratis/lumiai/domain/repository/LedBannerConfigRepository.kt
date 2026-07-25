package com.mejoresiagratis.lumiai.domain.repository

import com.mejoresiagratis.lumiai.domain.model.LedBannerConfig
import kotlinx.coroutines.flow.Flow

/** Preferencias del modo Letrero LED (persistidas). */
interface LedBannerConfigRepository {
    val config: Flow<LedBannerConfig>
    suspend fun setConfig(value: LedBannerConfig)
}
