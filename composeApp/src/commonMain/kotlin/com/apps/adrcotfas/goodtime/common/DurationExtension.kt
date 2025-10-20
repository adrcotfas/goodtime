/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.common

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

fun Duration.formatOverview(): String {
    val hours = this.inWholeHours
    val remMin = this.inWholeMinutes % 1.hours.inWholeMinutes

    return when {
        this.inWholeMinutes == 0L -> "0min"
        else ->
            buildString {
                if (hours != 0L) append("${hours}h ")
                if (remMin != 0L) append("${remMin}min")
            }.trim()
    }
}

object Time {
    fun startOfTodayAdjusted(secondOfDay: Int): Long {
        val dateTime =
            currentDateTime().apply {
                if (time.toSecondOfDay() < secondOfDay) {
                    minus(DatePeriod(days = 1))
                }
            }
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = dateTime.date.atStartOfDayIn(timeZone)
        return startOfDay.toEpochMilliseconds() + secondOfDay.seconds.inWholeMilliseconds
    }

    fun startOfThisWeekAdjusted(
        startDayOfWeek: DayOfWeek,
        secondOfDay: Int,
    ): LocalDateTime {
        val dateTime =
            currentDateTime().apply {
                if (time.toSecondOfDay() < secondOfDay) {
                    minus(DatePeriod(days = 1))
                }
            }
        val timeZone = TimeZone.currentSystemDefault()
        var date = dateTime.date
        while (date.dayOfWeek != startDayOfWeek) {
            date = date.minus(1, DateTimeUnit.DAY)
        }

        val startOfWeekInstant = date.atStartOfDayIn(timeZone).plus(secondOfDay.seconds)
        return startOfWeekInstant.toLocalDateTime(timeZone)
    }

    fun startOfThisMonth(secondOfDay: Int): LocalDateTime {
        val dateTime =
            currentDateTime().apply {
                if (time.toSecondOfDay() < secondOfDay) {
                    minus(DatePeriod(days = 1))
                }
            }
        val date =
            LocalDate(
                dateTime.date.year,
                dateTime.date.month,
                1,
            )
        val startOfMonthInstant = date.atStartOfDayIn(TimeZone.currentSystemDefault()).plus(secondOfDay.seconds)
        return startOfMonthInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    fun currentDateTime(): LocalDateTime {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toEpochMilliseconds()
        val currentInstant = Instant.fromEpochMilliseconds(now)
        return currentInstant.toLocalDateTime(timeZone)
    }

    fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        val timeZone = TimeZone.currentSystemDefault()
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        return instant.toLocalDateTime(timeZone)
    }
}

private fun firstWeekInYearStart(year: Int): LocalDate {
    val jan1st = LocalDate(year, 1, 1)
    val previousMonday = jan1st.minus(jan1st.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return if (jan1st.dayOfWeek <= DayOfWeek.THURSDAY) {
        previousMonday
    } else {
        previousMonday.plus(
            1,
            DateTimeUnit.WEEK,
        )
    }
}

fun LocalDateTime.plus(datePeriod: DatePeriod): LocalDateTime {
    val date = date
    val time = time
    return date.plus(datePeriod).atTime(time)
}

fun LocalDateTime.minus(datePeriod: DatePeriod): LocalDateTime {
    val date = date
    val time = time
    return date.minus(datePeriod).atTime(time)
}

fun LocalDate.isoWeekNumber(): Int {
    if (firstWeekInYearStart(year + 1) < this) return 1
    val currentYearStart = firstWeekInYearStart(year)
    val start = if (this < currentYearStart) firstWeekInYearStart(year - 1) else currentYearStart
    val currentCalendarWeek = start.until(this, DateTimeUnit.WEEK) + 1
    return currentCalendarWeek
}

fun LocalDate.at(firstDayOfWeek: DayOfWeek): LocalDate {
    var date = this
    while (date.dayOfWeek != firstDayOfWeek) {
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return date
}

fun LocalDate.firstDayOfWeekInMonth(startDayOfWeek: DayOfWeek): LocalDate {
    val firstDayOfMonth = LocalDate(year, month, 1)
    var date = firstDayOfMonth
    while (date.dayOfWeek != startDayOfWeek) {
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return date
}

fun LocalDate.firstDayOfWeekInThisWeek(startDayOfWeek: DayOfWeek): LocalDate {
    var date = this
    while (date.dayOfWeek != startDayOfWeek) {
        date = date.minus(1, DateTimeUnit.DAY)
    }
    return date
}

fun LocalDate.endOfWeekInThisWeek(startDayOfWeek: DayOfWeek): LocalDate {
    var date = this
    if (date.dayOfWeek == startDayOfWeek) {
        return date.plus(DatePeriod(days = 6))
    }
    while (date.dayOfWeek != startDayOfWeek) {
        date = date.plus(DatePeriod(days = 1))
    }
    date.minus(DatePeriod(days = 1))
    return date
}

fun LocalDate.firstDayOfThisQuarter(): LocalDate {
    val firstMonthInQuarter =
        when (quarter) {
            1 -> 1
            2 -> 4
            3 -> 7
            4 -> 10
            else -> throw IllegalArgumentException("Invalid quarter: $quarter. Valid values are 1-4.")
        }
    return LocalDate(year, firstMonthInQuarter, 1)
}

val LocalDate.quarter get() = (monthNumber - 1) / 3 + 1

fun LocalDate.toEpochMilliseconds(): Long = this.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun LocalDateTime.toEpochMilliseconds(): Long = this.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

inline fun <reified T : Enum<T>> T.entriesStartingWithThis(): List<T> {
    val entries = enumValues<T>()
    val index = entries.indexOf(this)
    return entries.drop(index) + entries.take(index)
}
