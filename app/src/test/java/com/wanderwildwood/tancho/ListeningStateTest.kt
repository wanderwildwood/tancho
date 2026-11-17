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
    fun aHeldBackRowIsNeverFlushedOverANewerRunOfTheSameBird() {
        val state = ListeningState()
        state.record(3, "Wren", "Troglodytes", 0.5f, 1_000)
        state.record(3, "Wren", "Troglodytes", 0.5f, 5_000) // same look: held back
        state.record(3, "Wren", "Troglodytes", 0.5f, 70_000) // over a minute later: a new row
        state.flush()
        val rows = state.heard.value
        assertEquals(listOf(70_000L, 1_000L), rows.map { it.firstHeard })
        assertEquals(1, rows[0].times)
        assertEquals(2, rows[1].times)
        assertEquals(5_000L, rows[1].lastHeard)
    }

    @Test
    fun aHeldBackRowIsNotFlushedOverAnotherBird() {
        val state = ListeningState()
        state.record(3, "Wren", "Troglodytes", 0.5f, 1_000)
        state.record(3, "Wren", "Troglodytes", 0.5f, 5_000) // held back
        state.record(4, "Robin", "Erithacus", 0.6f, 6_000)
        state.flush()
        val rows = state.heard.value
        assertEquals(listOf(4, 3), rows.map { it.speciesIndex })
        assertEquals(rows.size, rows.map { it.firstHeard }.toSet().size)
        assertEquals(2, rows[1].times)
    }

    @Test
    fun silentRepeatsAllCountAndFlushShowsThem() {
        val state = ListeningState()
        state.record(3, "Wren", "Troglodytes", 0.5f, 1_000)
        state.record(3, "Wren", "Troglodytes", 0.5f, 5_000)
        state.record(3, "Wren", "Troglodytes", 0.5f, 9_000)
        assertEquals(1, state.heard.value[0].times) // nothing redrawn yet
        state.flush()
        val rows = state.heard.value
        assertEquals(1, rows.size)
        assertEquals(3, rows[0].times)
        assertEquals(9_000L, rows[0].lastHeard)
    }

    @Test
    fun theWhereAndWhenChoiceCyclesAndWraps() {
        assertEquals(PlaceAndDate.WEIGHED, PlaceAndDate.IGNORED.next())
        assertEquals(PlaceAndDate.IGNORED, PlaceAndDate.DECISIVE.next())
        assertEquals(PlaceAndDate.WEIGHED, PlaceAndDate.nearest(0.55f))
    }
}
