package me.ankitgrai.kaizen.domain

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageFormatTest {
    private val tuesday = LocalDate.of(2026, 11, 17)
    private val locale = Locale.US

    @Test
    fun formatDurationOmitsZeroHours() {
        assertEquals("0m", formatDuration(0L))
        assertEquals("0m", formatDuration(59_999L))
        assertEquals("1m", formatDuration(60_000L))
        assertEquals("1h 0m", formatDuration(3_600_000L))
        assertEquals("1h 1m", formatDuration(3_660_000L))
    }

    @Test
    fun formatDurationCanKeepZeroHours() {
        assertEquals("0h 30m", formatDuration(1_800_000L, alwaysShowHours = true))
    }

    @Test
    fun categorizeAppUsesKeywordOrder() {
        assertEquals("Social", categorizeApp("com.instagram.android", "Instagram"))
        assertEquals("Video", categorizeApp("com.google.android.youtube", "YouTube"))
        assertEquals("Video", categorizeApp("com.google.android.apps.youtube.music", "YT Music"))
        assertEquals("Music", categorizeApp("com.spotify.music", "Spotify"))
        assertEquals("Games", categorizeApp("com.miHoYo.GenshinImpact", "Genshin"))
        assertEquals("Work", categorizeApp("com.Slack", "Slack"))
        assertEquals("Other", categorizeApp("com.android.settings", "Settings"))
    }

    @Test
    fun initialsTakeTwoWords() {
        assertEquals("YT", initials("You Tube"))
        assertEquals("I", initials("instagram"))
        assertEquals("?", initials("   "))
    }

    @Test
    fun weekStartsOnMonday() {
        assertEquals(LocalDate.of(2026, 11, 16), weekDates(tuesday).first())
        assertEquals(LocalDate.of(2026, 11, 22), weekDates(tuesday).last())
        assertEquals(7, weekDates(tuesday).size)
    }

    @Test
    fun periodShiftAndAdvance() {
        assertEquals(tuesday.minusWeeks(1), shiftPeriod(tuesday, UsagePeriod.WEEK, -1))
        assertEquals(tuesday.plusMonths(1), shiftPeriod(tuesday, UsagePeriod.MONTH, 1))
        assertFalse(canAdvance(tuesday, UsagePeriod.DAY, tuesday))
        assertTrue(canAdvance(tuesday.minusDays(1), UsagePeriod.DAY, tuesday))
    }

    @Test
    fun periodLabelUsesLocale() {
        assertEquals("17 Nov 2026", periodLabel(tuesday, UsagePeriod.DAY, locale))
        assertEquals("16 Nov – 22 Nov", periodLabel(tuesday, UsagePeriod.WEEK, locale))
        assertEquals("November 2026", periodLabel(tuesday, UsagePeriod.MONTH, locale))
    }

    @Test
    fun futureHourIsLaterTodayOnly() {
        val now = LocalTime.of(14, 30)
        assertTrue(isFutureHour(tuesday, 15, tuesday, now))
        assertFalse(isFutureHour(tuesday, 14, tuesday, now))
        assertFalse(isFutureHour(tuesday.minusDays(1), 23, tuesday, now))
    }
}
