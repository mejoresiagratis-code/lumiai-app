package com.mejoresiagratis.lumiai.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Umbral ÚNICO de "poca altura": apaisado en móvil, multiventana estrecha y plegables a media
 * apertura. El criterio es la altura DISPONIBLE, no la orientación nominal.
 *
 * Todas las pantallas deben leer [isCompactHeight] en lugar de comparar `screenHeightDp`
 * por su cuenta: antes de esta utilidad el umbral estaba duplicado en cuatro ficheros
 * (BeamHub, ScreenLight, Auth, Onboarding) y cualquier ajuste futuro los habría hecho
 * divergir — cada pantalla cambiando de disposición a una altura distinta (memo, checklist #30).
 */
const val COMPACT_HEIGHT_THRESHOLD_DP = 480

@Composable
@ReadOnlyComposable
fun isCompactHeight(): Boolean =
    LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_THRESHOLD_DP
