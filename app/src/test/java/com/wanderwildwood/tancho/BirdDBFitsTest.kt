package com.wanderwildwood.tancho

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A backup's database is swapped in only when it is a log this version can open. */
class BirdDBFitsTest {
    private val all = BirdDBHelper.COLUMNS

    @Test
    fun thisVersionWithEveryColumnFits() {
        assertTrue(BirdDBHelper.fits(1, all))
        assertTrue(BirdDBHelper.fits(1, all.map { it.uppercase() }))
    }

    @Test
    fun anotherVersionDoesNot() {
        assertFalse(BirdDBHelper.fits(0, all))
        assertFalse(BirdDBHelper.fits(2, all))
    }

    @Test
    fun aMissingTableOrColumnDoesNot() {
        assertFalse(BirdDBHelper.fits(1, emptyList()))
        assertFalse(BirdDBHelper.fits(1, all.drop(1)))
        assertFalse(BirdDBHelper.fits(1, listOf("id", "note")))
    }
}
