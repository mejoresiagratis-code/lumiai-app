package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Escala de forma con caracter M3 Expressive (Via A · toolchain actual).
// Radios mas generosos y consistentes; se mantienen los nombres existentes
// (small/medium/large) y se anaden extraSmall/extraLarge para la escala completa.
val LumiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)
