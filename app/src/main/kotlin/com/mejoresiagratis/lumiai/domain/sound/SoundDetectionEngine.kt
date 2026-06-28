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
            if (score >= config.threshold(category)) {
                val newStreak = (streak[category] ?: 0) + 1
                streak[category] = newStreak
                val last = lastFiredAtMs[category]
                val cooled = last == null || nowMs - last >= cooldownMs
                if (newStreak >= debounceWindows && cooled) {
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

    /** Reinicia el estado interno (al parar o reconfigurar la escucha). */
    fun reset() {
        streak.clear()
        lastFiredAtMs.clear()
    }
}
