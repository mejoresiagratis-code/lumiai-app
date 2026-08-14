package com.mejoresiagratis.lumiai.domain.sound

/**
 * Decide cuando una categoria debe disparar una alerta a partir del flujo de ventanas de
 * clasificacion. Logica pura (sin Android, sin micrófono): testeable al 100% en CI.
 *
 * Por cada categoria activa mantiene:
 *  - una racha de ventanas consecutivas por encima de su umbral ([debounceWindows]) para evitar
 *    disparos por un pico aislado de ruido;
 *  - un instante de ultimo disparo, para no encadenar avisos del mismo evento ([cooldownMs]).
 *
 * El umbral por categoria sale de [SoundAlertConfig.threshold] (segun la sensibilidad).
 */
class SoundDetectionEngine(
    private val config: SoundAlertConfig,
    private val matcher: SoundLabelMatcher = SoundLabelMatcher(),
    private val debounceWindows: Int = 2,
    private val cooldownMs: Long = 4_000L
) {
    private val streak = mutableMapOf<SoundCategory, Int>()
    private val lastFiredAtMs = mutableMapOf<SoundCategory, Long>()

    /**
     * Procesa una ventana de clasificacion ([scores]: etiqueta -> probabilidad) y devuelve las
     * categorias que deben alertar en este instante. [nowMs] es el reloj monotono de la ventana.
     */
    fun onWindow(scores: Map<String, Float>, nowMs: Long): List<SoundCategory> {
        // Mejor score por categoria activa en esta ventana.
        val best = mutableMapOf<SoundCategory, Float>()
        for ((label, score) in scores) {
            val category = matcher.categoryFor(label) ?: continue
            if (!config.isEnabled(category)) continue
            val previous = best[category]
            if (previous == null || score > previous) best[category] = score
        }

        val fired = mutableListOf<SoundCategory>()
        for (category in SoundCategory.entries) {
            if (!config.isEnabled(category)) {
                streak[category] = 0
                continue
            }
            val score = best[category] ?: 0f
            // Los transitorios llegan con el score DILUIDO (un golpe de ~0.2 s pesa poco
            // dentro de una ventana de ~1 s): su umbral efectivo se alivia un 30%. El
            // cooldown sigue conteniendo falsos positivos encadenados (QA 14-ago).
            val effectiveThreshold = config.threshold(category) *
                (if (category.transientSound) TRANSIENT_THRESHOLD_RELIEF else 1f)
            if (score >= effectiveThreshold) {
                val newStreak = (streak[category] ?: 0) + 1
                streak[category] = newStreak
                val last = lastFiredAtMs[category]
                val cooled = last == null || nowMs - last >= cooldownMs
                // Transitorios (ladrido, golpe, timbre): 1 ventana basta — duran menos
                // que el debounce estandar y jamas dispararian con el (QA 14-ago).
                val required = if (category.transientSound) 1 else debounceWindows
                if (newStreak >= required && cooled) {
                    fired += category
                    lastFiredAtMs[category] = nowMs
                    streak[category] = 0
                }
            } else {
                streak[category] = 0
            }
        }
        return fired
    }

    private companion object {
        const val TRANSIENT_THRESHOLD_RELIEF = 0.7f
    }

    /** Reinicia el estado interno (al parar o reconfigurar la escucha). */
    fun reset() {
        streak.clear()
        lastFiredAtMs.clear()
    }
}
