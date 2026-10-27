package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningStateTest {

    @Test
    fun theMicrophoneIsAllowedUntilTheAppFindsOutOtherwise() {
        val state = ListeningState()
        assertTrue(state.micAllowed.value)
        state.setMicAllowed(false)
        assertFalse(state.micAllowed.value)
    }

    @Test
    fun theSameBirdSingingAgainFoldsIntoOneRow() {
        val state = ListeningState()
        state.record(3, "Wren", "Troglodytes", 0.5f, 1_000)
        state.record(3, "Wren", "Troglodytes", 0.9f, 5_000)
        state.flush()
        val rows = state.heard.value
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].times)
        assertEquals(0.9f, rows[0].bestProbability, 0.0001f)
    }

    @Test
    fun aDifferentBirdStartsANewRowAbove() {
        val state = ListeningState()
        state.record(3, "Wren", "Troglodytes", 0.5f, 1_000)
        state.record(4, "Robin", "Erithacus", 0.6f, 2_000)
        assertEquals(listOf(4, 3), state.heard.value.map { it.speciesIndex })
    }

    @Test
    fun theWhereAndWhenChoiceCyclesAndWraps() {
        assertEquals(PlaceAndDate.WEIGHED, PlaceAndDate.IGNORED.next())
        assertEquals(PlaceAndDate.IGNORED, PlaceAndDate.DECISIVE.next())
        assertEquals(PlaceAndDate.WEIGHED, PlaceAndDate.nearest(0.55f))
    }
}
