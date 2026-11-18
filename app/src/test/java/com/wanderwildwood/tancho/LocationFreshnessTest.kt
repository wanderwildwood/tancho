package com.wanderwildwood.tancho

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFreshnessTest {
    private val minute = 60_000L
    private val now = 1_000_000_000L

    @Test
    fun aFixFromAMinuteAgoIsStillGood() {
        assertTrue(LocationHelper.fixIsFresh(now - minute, now))
    }

    @Test
    fun aFixOlderThanThreeMinutesIsNoLongerThrownAway() {
        assertTrue(LocationHelper.fixIsFresh(now - 10 * minute, now))
    }

    @Test
    fun aFixIsGoodForThirtyMinutesAndNoLonger() {
        assertTrue(LocationHelper.fixIsFresh(now - 30 * minute, now))
        assertFalse(LocationHelper.fixIsFresh(now - 31 * minute, now))
    }

    @Test
    fun noFixAtAllIsNeverFresh() {
        assertFalse(LocationHelper.fixIsFresh(0, now))
    }

    @Test
    fun aStaleFixIsNotStampedOnADetection() {
        assertTrue(LocationHelper.stampable(false, now - 10 * minute, now))
        assertFalse(LocationHelper.stampable(false, now - 31 * minute, now))
        assertFalse(LocationHelper.stampable(false, 0, now))
    }

    @Test
    fun aTypedInPlaceDoesNotAge() {
        assertTrue(LocationHelper.stampable(true, 0, now))
    }
}
