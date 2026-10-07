package io.github.daisukikaffuchino.han1meviewer.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackTimeTest {

    @Test
    fun `formats mm ss under one hour`() {
        assertEquals("00:00", formatPlaybackTime(0L))
        assertEquals("00:59", formatPlaybackTime(59_000L))
        assertEquals("01:00", formatPlaybackTime(60_000L))
        assertEquals("59:59", formatPlaybackTime(3_599_000L))
    }

    @Test
    fun `formats h mm ss at one hour and above`() {
        assertEquals("1:00:00", formatPlaybackTime(3_600_000L))
        assertEquals("1:01:01", formatPlaybackTime(3_661_000L))
        assertEquals("10:00:00", formatPlaybackTime(36_000_000L))
    }

    @Test
    fun `negative position is clamped to zero`() {
        assertEquals("00:00", formatPlaybackTime(-1_000L))
    }
}
