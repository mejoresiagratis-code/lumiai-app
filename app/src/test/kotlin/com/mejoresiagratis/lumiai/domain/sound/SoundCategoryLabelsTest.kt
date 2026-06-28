package com.mejoresiagratis.lumiai.domain.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que TODA etiqueta declarada en [SoundCategory.labels] existe, carácter a carácter, en
 * el mapa de clases de YAMNet (AudioSet). Si una etiqueta no coincide exactamente, esa clase nunca
 * dispararía en el dispositivo aunque el resto funcione; este test convierte ese fallo silencioso
 * en un fallo de CI.
 *
 * El recurso `yamnet_labels.txt` (una clase por línea) se generó desde el `yamnet_class_map.csv`
 * oficial de Google/TensorFlow (521 clases, columna `display_name`). Si en F5 se cambia de modelo,
 * regenerar ese recurso desde el mapa del nuevo modelo.
 */
class SoundCategoryLabelsTest {

    private val officialLabels: Set<String> by lazy {
        val stream = javaClass.getResourceAsStream("/yamnet_labels.txt")
            ?: error("No se encontró /yamnet_labels.txt en el classpath de test")
        stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    @Test
    fun `el recurso del mapa tiene las 521 clases de YAMNet`() {
        assertEquals(521, officialLabels.size)
    }

    @Test
    fun `cada etiqueta de cada categoria existe char-a-char en el mapa de YAMNet`() {
        val faltantes = SoundCategory.entries.flatMap { category ->
            category.labels.filter { it !in officialLabels }.map { "${category.name}: '$it'" }
        }
        assertTrue(
            "Etiquetas que NO existen en el mapa de YAMNet (no dispararán nunca): $faltantes",
            faltantes.isEmpty()
        )
    }

    @Test
    fun `ninguna categoria se queda sin etiquetas`() {
        SoundCategory.entries.forEach {
            assertTrue("${it.name} no declara etiquetas", it.labels.isNotEmpty())
        }
    }
}
