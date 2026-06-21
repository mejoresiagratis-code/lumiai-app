package com.mejoresiagratis.lumiai.domain.flash

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MorseTest {
    @Test
    fun `sos produces the exact international timing for unit 100ms`() {
        val expected = longArrayOf(
            100, 100, 100, 100, 100,
            300,
            300, 100, 300, 100, 300,
            300,
            100, 100, 100, 100, 100
        )
        assertArrayEquals(expected, Morse.sos(100))
    }

    @Test
    fun `durations alternate starting with an ON element`() {
        val d = Morse.sos(100)
        assertArrayEquals(longArrayOf(100, 100, 100, 100, 100), longArrayOf(d[0], d[2], d[4], d[12], d[16]))
    }
}
