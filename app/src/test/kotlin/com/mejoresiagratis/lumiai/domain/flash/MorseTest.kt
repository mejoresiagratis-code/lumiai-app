package com.mejoresiagratis.lumiai.domain.flash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTest {
    @Test fun `E es un punto (1u ON)`() {
        assertArrayEquals(longArrayOf(100), Morse.toDurations("E", 100))
    }
    @Test fun `T es una raya (3u ON)`() {
        assertArrayEquals(longArrayOf(300), Morse.toDurations("T", 100))
    }
    @Test fun `EE separa caracteres con 3u OFF`() {
        assertArrayEquals(longArrayOf(100, 300, 100), Morse.toDurations("EE", 100))
    }
    @Test fun `espacio entre palabras es 7u`() {
        assertArrayEquals(longArrayOf(100, 700, 100), Morse.toDurations("E E", 100))
    }
    @Test fun `ignora caracteres no soportados`() {
        assertArrayEquals(Morse.toDurations("E", 100), Morse.toDurations("E@#", 100))
    }
    @Test fun `texto vacio o sin codigo da array vacio`() {
        assertEquals(0, Morse.toDurations("", 100).size)
        assertEquals(0, Morse.toDurations("@@@", 100).size)
    }
    @Test fun `sos sigue funcionando`() {
        assertTrue(Morse.sos(100).isNotEmpty())
    }
}
