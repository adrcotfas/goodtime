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

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

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
                dayOfMonth()
                char('-')
                hour()
                minute()
            }
        return format.format(dateTime)
    }

    fun Long.formatToIso8601(): String {
        val instant = Instant.fromEpochMilliseconds(this)

        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val format = LocalDateTime.Formats.ISO
        return format.format(dateTime)
    }
}
