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
package com.apps.adrcotfas.goodtime.settings.reminders

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object ReminderTimeCalculator {
    /**
     * Calculates the next reminder time in milliseconds since epoch.
     *
     * @param currentTimeMillis The current time in milliseconds since epoch
     * @param reminderDay The day of the week for the reminder
     * @param secondOfDay The time of day in seconds (0-86399)
     * @param timeZone The time zone to use for calculations
     * @return The next reminder time in milliseconds since epoch
     */
    fun calculateNextReminderTime(
        currentTimeMillis: Long,
        reminderDay: DayOfWeek,
        secondOfDay: Int,
        timeZone: TimeZone,
    ): Long {
        val now = Instant.fromEpochMilliseconds(currentTimeMillis).toLocalDateTime(timeZone)

        val time = LocalTime.fromSecondOfDay(secondOfDay)
        var reminderTime =
            LocalDateTime(
                now.year,
                now.month,
                now.day,
                time.hour,
                time.minute,
                0,
                0,
            )

        val currentDayNumber = now.dayOfWeek.isoDayNumber
        val targetDayNumber = reminderDay.isoDayNumber
        val daysUntilTarget = (targetDayNumber - currentDayNumber + 7) % 7

        // Add days to reach the target day of week
        if (daysUntilTarget > 0) {
            reminderTime =
                reminderTime.date
                    .plus(daysUntilTarget, DateTimeUnit.DAY)
                    .atTime(reminderTime.time)
        }

        // If the reminder time is in the past or equal to now, schedule for next week
        if (reminderTime <= now) {
            reminderTime =
                reminderTime.date
                    .plus(7, DateTimeUnit.DAY)
                    .atTime(reminderTime.time)
        }

        return reminderTime.toInstant(timeZone).toEpochMilliseconds()
    }
}
