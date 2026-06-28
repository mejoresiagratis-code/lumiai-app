package com.mejoresiagratis.lumiai.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Tokens de movimiento del lavado de cara M3 Expressive (Via A, con Compose actual).
 *
 * - [emphasized]: muelle con rebote para gestos espaciales protagonistas (orbe, formas que mutan).
 * - [standard]: muelle que asienta con poco rebote (cambios de tamano/posicion sobrios).
 * - [effects]: sin rebote, para color/alfa/brillo (evita oscilacion en valores no espaciales).
 *
 * Se usan como `animateXAsState(..., animationSpec = LumiMotion.emphasized())`.
 */
object LumiMotion {
    fun <T> emphasized(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)

    fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)

    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
}
