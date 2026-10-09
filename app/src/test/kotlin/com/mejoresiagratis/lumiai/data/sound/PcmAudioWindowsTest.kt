package com.mejoresiagratis.lumiai.data.sound

import com.google.mediapipe.tasks.components.containers.AudioData
import org.junit.Assert.*
import org.junit.Test

class PcmAudioWindowsTest {
    @Test fun partialReadsDoNotEmitPaddedWindows() {
        val windows = PcmAudioWindows(6, 3)
        val output = mutableListOf<ShortArray>()
        windows.append(shortArrayOf(1, 2), 2, output::add)
        windows.append(shortArrayOf(3, 4, 99), 2, output::add)
        assertTrue(output.isEmpty())
        windows.append(shortArrayOf(5, 6), 2, output::add)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4, 5, 6), output.single())
    }

    @Test fun overlapPreservesThePreviousHalfInOrder() {
        val windows = PcmAudioWindows(6, 3)
        val output = mutableListOf<ShortArray>()
        windows.append(ShortArray(12) { (it + 1).toShort() }, 12, output::add)
        assertEquals(3, output.size)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4, 5, 6), output[0])
        assertArrayEquals(shortArrayOf(4, 5, 6, 7, 8, 9), output[1])
        assertArrayEquals(shortArrayOf(7, 8, 9, 10, 11, 12), output[2])
    }

    @Test fun emptyReadsDoNotRepeatOrAdvanceClassification() {
        val windows = PcmAudioWindows(4, 2)
        var results = 0
        windows.append(shortArrayOf(1, 2, 3, 4), 4) { results++ }
        repeat(10) { windows.append(shortArrayOf(99), 0) { results++ } }
        assertEquals(1, results)
        windows.append(shortArrayOf(5, 6), 2) { results++ }
        assertEquals(2, results)
    }

    @Test fun arbitraryReadBoundariesMatchTheOriginalSignal() {
        val source = ShortArray(39_000) { ((it % 30_000) + 1).toShort() }
        val windows = PcmAudioWindows()
        val output = mutableListOf<ShortArray>()
        var offset = 0
        while (offset < source.size) {
            val count = minOf(997, source.size - offset)
            windows.append(source.copyOfRange(offset, offset + count), count, output::add)
            offset += count
        }
        assertEquals(4, output.size)
        output.forEachIndexed { index, actual ->
            assertArrayEquals(source.copyOfRange(index * 7_800, index * 7_800 + 15_600), actual)
        }
    }

    @Test fun completePcmWindowReachesMediaPipeWithoutZeroPadding() {
        val format = AudioData.AudioDataFormat.builder().setNumOfChannels(1).setSampleRate(16_000f).build()
        val windows = PcmAudioWindows()
        var classified = false
        repeat(39) {
            windows.append(ShortArray(400) { 16_384 }, 400) { samples ->
                val audio = AudioData.create(format, samples.size)
                audio.load(samples)
                assertEquals(15_600, audio.buffer.size)
                assertTrue(audio.buffer.all { it > 0.49f && it < 0.51f })
                classified = true
            }
        }
        assertTrue(classified)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidReadCountIsRejected() {
        PcmAudioWindows().append(ShortArray(1), 2) { fail() }
    }
}
