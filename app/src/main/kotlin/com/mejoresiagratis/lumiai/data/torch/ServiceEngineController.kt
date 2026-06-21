package com.mejoresiagratis.lumiai.data.torch

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceEngineController @Inject constructor(
    @ApplicationContext private val context: Context
) : EngineController {

    override fun start() {
        TorchService.ensureChannel(context)
        ContextCompat.startForegroundService(context, Intent(context, TorchService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, TorchService::class.java))
    }
}
