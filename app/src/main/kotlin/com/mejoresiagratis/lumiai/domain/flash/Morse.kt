package com.mejoresiagratis.lumiai.domain.flash

import java.text.Normalizer

/**
 * Convierte texto a una lista plana de duraciones on/off (ms) segun timing Morse internacional:
 * dot=1u, dash=3u, hueco intra-caracter=1u, hueco inter-caracter=3u, hueco entre palabras=7u.
 * Indices pares = ON, impares = OFF. La lista empieza y termina en ON.
 */
object Morse {
    private val TABLE: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )

    fun toDurations(text: String, unitMs: Long): LongArray {
        val u = unitMs
        val out = ArrayList<Long>()
        var firstChar = true
        for (raw in normalize(text).uppercase()) {
            if (raw == ' ') {
                if (out.isNotEmpty()) out.add(7 * u)
                firstChar = true
                continue
            }
            val code = TABLE[raw] ?: continue
            if (!firstChar) out.add(3 * u)
            firstChar = false
            for ((i, sym) in code.withIndex()) {
                out.add(if (sym == '-') 3 * u else u)
                if (i != code.lastIndex) out.add(u)
            }
        }
        return out.toLongArray()
    }

    fun sos(unitMs: Long): LongArray = toDurations("SOS", unitMs)

    /**
     * Quita diacríticos (á→a, ñ→n, ü→u, ç→c…) descomponiendo en NFD y eliminando las
     * marcas combinantes, para no perder caracteres acentuados al codificar a Morse.
     */
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    /** Caracteres del texto que no tienen representación Morse (se omitirán al emitir). */
    fun unsupportedChars(text: String): Set<Char> =
        normalize(text).uppercase()
            .filter { it != ' ' && !TABLE.containsKey(it) }
            .toSet()
}
