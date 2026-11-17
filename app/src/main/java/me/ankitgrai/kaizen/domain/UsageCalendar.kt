package me.ankitgrai.kaizen.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class UsagePeriod {
    DAY,
    WEEK,
    MONTH,
    ;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

fun LocalDate.formatWeekdayDate(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEEE, MMM d", locale))

fun LocalDate.formatShortWeekdayDate(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))

fun LocalDate.formatDayMonth(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("dd MMM", locale))

fun LocalDate.formatShortDayMonth(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("d MMM", locale))

fun LocalDate.formatDayMonthYear(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("dd MMM yyyy", locale))

fun LocalDate.formatMonthYear(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

fun LocalDate.formatWeekdayNarrow(locale: Locale): String =
    dayOfWeek.getDisplayName(TextStyle.NARROW, locale)

fun LocalDate.formatWeekdayShort(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EE", locale))

fun LocalDate.formatWeekdayAbbrev(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEE", locale))

fun hourTickLabel(hour: Int, locale: Locale): String =
    LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h", locale))

fun formatHourRange(hour: Int, locale: Locale): String {
    val start = LocalTime.of(hour, 0)
    val end = start.plusHours(1)
    val pattern = DateTimeFormatter.ofPattern("h:mm a", locale)
    return "${start.format(pattern)} – ${end.format(pattern)}"
}

fun isFutureHour(
    date: LocalDate,
    hour: Int,
    today: LocalDate,
    now: LocalTime = LocalTime.now(),
): Boolean = date == today && hour > now.hour

fun weekDates(anchor: LocalDate): List<LocalDate> {
    val start = anchor.with(DayOfWeek.MONDAY)
    return (0..6).map { start.plusDays(it.toLong()) }
}

fun monthDates(anchor: LocalDate): List<LocalDate> =
    (1..anchor.lengthOfMonth()).map { anchor.withDayOfMonth(it) }

fun chartDates(period: UsagePeriod, anchor: LocalDate): List<LocalDate> = when (period) {
    UsagePeriod.DAY -> listOf(anchor)
    UsagePeriod.WEEK -> weekDates(anchor)
    UsagePeriod.MONTH -> monthDates(anchor)
}

fun shiftPeriod(date: LocalDate, period: UsagePeriod, steps: Long): LocalDate = when (period) {
    UsagePeriod.DAY -> date.plusDays(steps)
    UsagePeriod.WEEK -> date.plusWeeks(steps)
    UsagePeriod.MONTH -> date.plusMonths(steps)
}

fun canAdvance(date: LocalDate, period: UsagePeriod, today: LocalDate): Boolean =
    !shiftPeriod(date, period, 1).isAfter(today)

fun periodLabel(date: LocalDate, period: UsagePeriod, locale: Locale): String = when (period) {
    UsagePeriod.DAY -> date.formatDayMonthYear(locale)
    UsagePeriod.WEEK -> {
        val start = date.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        "${start.formatDayMonth(locale)} – ${end.formatDayMonth(locale)}"
    }
    UsagePeriod.MONTH -> date.formatMonthYear(locale)
}
