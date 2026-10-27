package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class TimeTextTest {
    private val utc = TimeZone.getTimeZone("UTC")

    // 14:05 UTC on 2 January 1970.
    private val afternoon = (24L * 60 * 60 + 14 * 3600 + 5 * 60) * 1000

    @Test
    fun twentyFourHourClockHasLeadingZeroAndNoSuffix() {
        assertEquals("14:05", TimeText.time(afternoon, true, Locale.US, utc))
        assertEquals("00:30", TimeText.time(30 * 60 * 1000L, true, Locale.US, utc))
    }

    @Test
    fun twelveHourClockUsesAmPm() {
        assertEquals("2:05 PM", TimeText.time(afternoon, false, Locale.US, utc))
        assertEquals("12:30 AM", TimeText.time(30 * 60 * 1000L, false, Locale.US, utc))
    }

    @Test
    fun dateFollowsTheLocalesOrder() {
        assertEquals("1/2/70", TimeText.date(afternoon, Locale.US, utc))
        assertEquals("02/01/1970", TimeText.date(afternoon, Locale.UK, utc))
    }

    @Test
    fun theSamePatternIsUsedEverywhere() {
        assertEquals("HH:mm", TimeText.timePattern(true))
        assertEquals("h:mm a", TimeText.timePattern(false))
    }
}
