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
package com.apps.adrcotfas.goodtime.bl

import io.github.adrcotfas.datetime.names.FormatStyle
import io.github.adrcotfas.datetime.names.TextStyle
import io.github.adrcotfas.datetime.names.format
import io.github.adrcotfas.datetime.names.getDisplayName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object TimeUtils {
    fun Long.formatMilliseconds(minutesOnly: Boolean = false): String {
        val totalSeconds = (this / 1000).run { if (minutesOnly) this + 59 else this }
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()
        val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()
        return if (minutesOnly) {
            minutesString
        } else {
            "$minutesString:$secondsString"
        }
    }

    fun Long.formatForBackupFileName(): String {
        val instant = Instant.fromEpochMilliseconds(this)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val format =
            LocalDateTime.Format {
                year()
                monthNumber()
                day()
                char('-')
                hour()
                minute()
            }
        return format.format(dateTime)
    }

    fun Long.formatToIso8601(): String {
        val instant = Instant.fromEpochMilliseconds(this)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val format =
            LocalDateTime.Format {
                date(LocalDate.Formats.ISO)
                char('T')
                hour()
                char(':')
                minute()
            }
        return format.format(dateTime)
    }

    fun formatDateTime(
        millis: Long,
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.SHORT,
    ): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return dateTime.format(dateStyle, timeStyle)
    }

    fun formatDate(
        millis: Long,
        formatStyle: FormatStyle = FormatStyle.MEDIUM,
    ): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date.format(formatStyle)
    }

    fun formatTime(
        millis: Long,
        formatStyle: FormatStyle = FormatStyle.SHORT,
    ): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
        return time.format(formatStyle)
    }

    fun localizedMonthNamesFull() = Month.entries.map { it.getDisplayName(textStyle = TextStyle.FULL_STANDALONE) }

    fun getLocalizedDayNamesForStats(): List<String> {
        val localizedDayNamesShort =
            DayOfWeek.entries.map { it.getDisplayName(textStyle = TextStyle.SHORT_STANDALONE) }
        return if (localizedDayNamesShort.any { it.length > 3 }) {
            val localizedDayNamesNarrow =
                DayOfWeek.entries.map { it.getDisplayName(textStyle = TextStyle.NARROW_STANDALONE) }
            return localizedDayNamesNarrow
        } else {
            localizedDayNamesShort
        }
    }

    fun getLocalizedMonthNamesForStats(): List<String> {
        val localizedMonthNamesShort =
            Month.entries.map { it.getDisplayName(textStyle = TextStyle.SHORT_STANDALONE) }
        return if (localizedMonthNamesShort.any { it.length > 3 }) {
            val localizedMonthNamesNarrow =
                Month.entries.map { it.getDisplayName(textStyle = TextStyle.NARROW_STANDALONE) }
            return localizedMonthNamesNarrow
        } else {
            localizedMonthNamesShort
        }
    }
}
