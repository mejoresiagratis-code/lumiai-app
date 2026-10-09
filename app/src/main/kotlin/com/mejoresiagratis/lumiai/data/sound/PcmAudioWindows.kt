package com.mejoresiagratis.lumiai.data.sound

/** Complete overlapping PCM windows. Partial microphone reads never introduce zero padding. */
internal class PcmAudioWindows(
    private val windowSize: Int = 15_600,
    private val hopSize: Int = 7_800
) {
    init {
        require(windowSize > 0)
        require(hopSize in 1..windowSize)
    }

    private val ring = ShortArray(windowSize)
    private var next = 0
    private var filled = 0
    private var sinceWindow = 0

    fun append(samples: ShortArray, count: Int, onWindow: (ShortArray) -> Unit) {
        require(count in 0..samples.size)
        for (i in 0 until count) {
            ring[next] = samples[i]
            next = (next + 1) % windowSize
            if (filled < windowSize) {
                filled++
                if (filled == windowSize) emit(onWindow)
            } else {
                sinceWindow++
                if (sinceWindow == hopSize) emit(onWindow)
            }
        }
    }

    private fun emit(onWindow: (ShortArray) -> Unit) {
        val window = ShortArray(windowSize)
        ring.copyInto(window, 0, next, windowSize)
        ring.copyInto(window, windowSize - next, 0, next)
        sinceWindow = 0
        onWindow(window)
    }
}
