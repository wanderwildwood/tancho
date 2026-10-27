package com.wanderwildwood.tancho

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The one place a time of day or a date is turned into words.
 *
 * The listening screen wrote a time as 14:05 whatever the phone was set to, the log did the same
 * under a date in the phone's own order, and sharing a hearing wrote 02:05 PM: three ways to
 * say one moment. Everything now asks here, and the clock follows the phone's 12 or 24 hour
 * setting.
 */
object TimeText {

    /** The pattern for a time of day: 14:05 on a 24 hour clock, 2:05 PM on a 12 hour one. */
    fun timePattern(is24Hour: Boolean): String = if (is24Hour) "HH:mm" else "h:mm a"

    fun time(
        millis: Long,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault(),
        zone: TimeZone = TimeZone.getDefault(),
    ): String = SimpleDateFormat(timePattern(is24Hour), locale).apply { timeZone = zone }.format(Date(millis))

    /** The short date in the phone's own order, as the log's day headings have always shown it. */
    fun date(
        millis: Long,
        locale: Locale = Locale.getDefault(),
        zone: TimeZone = TimeZone.getDefault(),
    ): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
        .apply { timeZone = zone }.format(Date(millis))
}

/** A time of day, on the phone's 12 or 24 hour clock. */
fun Context.timeText(millis: Long): String = TimeText.time(millis, DateFormat.is24HourFormat(this))

/** A short date, in the phone's order. */
fun Context.dateText(millis: Long): String = TimeText.date(millis)
