package com.mejoresiagratis.lumiai.domain.sound

/** Respect the selected outputs even when a permission or device capability is unavailable. */
suspend fun AlertChannel.deliverOutputs(
    hasFlash: Boolean,
    showScreen: suspend () -> Unit,
    flash: suspend () -> Unit
) {
    if (usesScreen) showScreen()
    if (usesFlash && hasFlash) flash()
}
