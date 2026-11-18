package com.wanderwildwood.tancho

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WavSweepTest {
    private val since = 1_800_000_000_000L
    private val old = since - 60_000L
    private val logged = setOf(1_700_000_000_000L)

    @Test fun `a recording the log names stays`() {
        assertFalse(WavUtils.isOrphan("1700000000000.wav", old, since, logged))
    }

    @Test fun `an old recording nothing names goes`() {
        assertTrue(WavUtils.isOrphan("1700000000001.wav", old, since, logged))
    }

    @Test fun `a recording written after the snapshot stays, named or not`() {
        assertFalse(WavUtils.isOrphan("1800000000500.wav", since + 500, since, logged))
        assertFalse(WavUtils.isOrphan("1799999999500.wav", since - 500, since, logged))
    }

    @Test fun `an unfinished write goes once it is old, never while it may be being written`() {
        assertTrue(WavUtils.isOrphan("1700000000002" + WavUtils.PART, old, since, logged))
        assertFalse(WavUtils.isOrphan("1800000000000" + WavUtils.PART, since, since, logged))
    }

    @Test fun `files not named like ours are left alone`() {
        assertFalse(WavUtils.isOrphan("notes.txt", old, since, logged))
        assertFalse(WavUtils.isOrphan("song.wav", old, since, logged))
    }
}
