package com.mejoresiagratis.lumiai.ui.home.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.domain.model.FlashMode

data class ModeUi(
    val mode: FlashMode,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val isPro: Boolean = false,
    @StringRes val shortLabelRes: Int = labelRes
)

val MODE_CATALOG: List<ModeUi> = listOf(
    ModeUi(FlashMode.CONTINUOUS, R.string.mode_continuous, R.drawable.ic_mode_continuous),
    ModeUi(FlashMode.SCREEN, R.string.mode_screen, R.drawable.ic_mode_screen),
    ModeUi(FlashMode.SOS_MORSE, R.string.mode_sos, R.drawable.ic_mode_sos),
    ModeUi(FlashMode.STROBE, R.string.mode_strobe, R.drawable.ic_mode_strobe),
    ModeUi(FlashMode.TEXT_MORSE, R.string.mode_text_morse, R.drawable.ic_mode_morse_text, isPro = true, shortLabelRes = R.string.mode_text_morse_short)
)
