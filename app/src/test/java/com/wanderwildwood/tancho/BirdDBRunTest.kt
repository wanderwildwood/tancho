package com.wanderwildwood.tancho

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The condensed log folds one hearing into a row, never readings hours or days apart. */
class BirdDBRunTest {
    private val t = 1_700_000_000_000L

    @Test
    fun theSameBirdWithinAMinuteIsOneRun() {
        assertTrue(BirdDBHelper.sameRun(7, t, 7, t + 3_000))
        assertTrue(BirdDBHelper.sameRun(7, t, 7, t + BirdDBHelper.SAME_RUN_MS))
    }

    @Test
    fun aGapEndsTheRun() {
        assertFalse(BirdDBHelper.sameRun(7, t, 7, t + BirdDBHelper.SAME_RUN_MS + 1))
        assertFalse(BirdDBHelper.sameRun(7, t, 7, t + 12 * 60 * 60 * 1000L))
    }

    @Test
    fun anotherBirdEndsTheRun() {
        assertFalse(BirdDBHelper.sameRun(7, t, 8, t + 1_000))
    }
}
