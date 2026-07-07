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

import com.apps.adrcotfas.goodtime.settings.reminders.ReminderTimeCalculator.calculateNextReminderTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ReminderTimerCalculatorTest {
    private val timeZone = TimeZone.UTC

    private fun createDateTime(
        year: Int = 2024,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        LocalDateTime(year, month, day, hour, minute, 0, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()

    private fun Long.toDateTime(): LocalDateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

    @Test
    fun testSameDayFutureTime_schedulesSameDay() {
        // Monday at 15:00, alarm for Monday at 15:01
        val currentTime = createDateTime(month = 1, day = 15, hour = 15, minute = 0) // Monday
        val reminderSecond = 15 * 3600 + 1 * 60 // 15:01

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(15, resultDateTime.day) // Same day (Monday)
        assertEquals(15, resultDateTime.hour)
        assertEquals(1, resultDateTime.minute)
    }

    @Test
    fun testSameDayPastTime_schedulesNextWeek() {
        // Monday at 15:01, alarm for Monday at 15:00
        val currentTime = createDateTime(month = 1, day = 15, hour = 15, minute = 1) // Monday
        val reminderSecond = 15 * 3600 + 0 * 60 // 15:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(22, resultDateTime.day) // Next Monday
        assertEquals(15, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testSameDayExactTime_schedulesNextWeek() {
        // Monday at 15:00, alarm for Monday at 15:00 (exact same time)
        val currentTime = createDateTime(month = 1, day = 15, hour = 15, minute = 0) // Monday
        val reminderSecond = 15 * 3600 + 0 * 60 // 15:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(22, resultDateTime.day) // Next Monday
        assertEquals(15, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testFutureDayInSameWeek_schedulesThatDay() {
        // Monday at 15:00, alarm for Wednesday at 10:00
        val currentTime = createDateTime(month = 1, day = 15, hour = 15, minute = 0) // Monday
        val reminderSecond = 10 * 3600 + 0 * 60 // 10:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.WEDNESDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(17, resultDateTime.day) // Wednesday (2 days later)
        assertEquals(10, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testPastDayInWeek_schedulesNextWeek() {
        // Friday at 15:00, alarm for Monday at 10:00
        val currentTime = createDateTime(month = 1, day = 19, hour = 15, minute = 0) // Friday
        val reminderSecond = 10 * 3600 + 0 * 60 // 10:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(22, resultDateTime.day) // Next Monday (4 days later)
        assertEquals(10, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testSundayToMonday_schedulesNextMonday() {
        // Sunday at 20:00, alarm for Monday at 10:00
        val currentTime = createDateTime(month = 1, day = 21, hour = 20, minute = 0) // Sunday
        val reminderSecond = 10 * 3600 + 0 * 60 // 10:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(22, resultDateTime.day) // Next Monday (1 day later)
        assertEquals(10, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testMondayToSunday_schedulesSunday() {
        // Monday at 15:00, alarm for Sunday at 20:00
        val currentTime = createDateTime(month = 1, day = 15, hour = 15, minute = 0) // Monday
        val reminderSecond = 20 * 3600 + 0 * 60 // 20:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.SUNDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(21, resultDateTime.day) // Sunday (6 days later)
        assertEquals(20, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testSaturdayToSunday_schedulesSunday() {
        // Saturday at 10:00, alarm for Sunday at 9:00
        val currentTime = createDateTime(month = 1, day = 20, hour = 10, minute = 0) // Saturday
        val reminderSecond = 9 * 3600 + 0 * 60 // 09:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.SUNDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(21, resultDateTime.day) // Next day (Sunday)
        assertEquals(9, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testSaturdayToSundayFutureTime_schedulesSunday() {
        // Saturday at 10:00, alarm for Sunday at 9:59 (still in the future relative to Saturday 10:00)
        val currentTime = createDateTime(month = 1, day = 20, hour = 10, minute = 0) // Saturday
        val reminderSecond = 9 * 3600 + 59 * 60 // 09:59

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.SUNDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(21, resultDateTime.day) // Tomorrow (Sunday)
        assertEquals(9, resultDateTime.hour)
        assertEquals(59, resultDateTime.minute)
    }

    @Test
    fun testMidnight_schedulesMidnightNextOccurrence() {
        // Monday at 01:00, alarm for Monday at 00:00
        val currentTime = createDateTime(month = 1, day = 15, hour = 1, minute = 0) // Monday
        val reminderSecond = 0 // 00:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(22, resultDateTime.day) // Next Monday
        assertEquals(0, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testAlmostMidnight_schedulesLaterSameDay() {
        // Monday at 23:58, alarm for Monday at 23:59
        val currentTime = createDateTime(month = 1, day = 15, hour = 23, minute = 58) // Monday
        val reminderSecond = 23 * 3600 + 59 * 60 // 23:59

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.MONDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(15, resultDateTime.day) // Same day (Monday)
        assertEquals(23, resultDateTime.hour)
        assertEquals(59, resultDateTime.minute)
    }

    @Test
    fun testEndOfMonth_schedulesCorrectly() {
        // Monday Jan 29 at 15:00, alarm for Wednesday at 10:00
        val currentTime = createDateTime(month = 1, day = 29, hour = 15, minute = 0) // Monday
        val reminderSecond = 10 * 3600 + 0 * 60 // 10:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.WEDNESDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(1, resultDateTime.month.number)
        assertEquals(31, resultDateTime.day) // Wednesday Jan 31
        assertEquals(10, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testMonthBoundary_schedulesNextMonth() {
        // Wednesday Jan 31 at 15:00, alarm for Friday at 10:00
        val currentTime = createDateTime(month = 1, day = 31, hour = 15, minute = 0) // Wednesday
        val reminderSecond = 10 * 3600 + 0 * 60 // 10:00

        val result =
            calculateNextReminderTime(
                currentTimeMillis = currentTime,
                reminderDay = DayOfWeek.FRIDAY,
                secondOfDay = reminderSecond,
                timeZone = timeZone,
            )

        val resultDateTime = result.toDateTime()
        assertEquals(2024, resultDateTime.year)
        assertEquals(2, resultDateTime.month.number) // February
        assertEquals(2, resultDateTime.day) // Friday Feb 2
        assertEquals(10, resultDateTime.hour)
        assertEquals(0, resultDateTime.minute)
    }

    @Test
    fun testAllDaysOfWeek_schedulesCorrectly() {
        // Test scheduling from Monday for each day of the week
        val mondayTime = createDateTime(month = 1, day = 15, hour = 10, minute = 0) // Monday
        val reminderSecond = 14 * 3600 + 30 * 60 // 14:30

        val expectedDays =
            mapOf(
                DayOfWeek.MONDAY to 15, // Same day but later time
                DayOfWeek.TUESDAY to 16,
                DayOfWeek.WEDNESDAY to 17,
                DayOfWeek.THURSDAY to 18,
                DayOfWeek.FRIDAY to 19,
                DayOfWeek.SATURDAY to 20,
                DayOfWeek.SUNDAY to 21,
            )

        for ((dayOfWeek, expectedDay) in expectedDays) {
            val result =
                calculateNextReminderTime(
                    currentTimeMillis = mondayTime,
                    reminderDay = dayOfWeek,
                    secondOfDay = reminderSecond,
                    timeZone = timeZone,
                )

            val resultDateTime = result.toDateTime()
            assertEquals(expectedDay, resultDateTime.day, "Failed for $dayOfWeek")
            assertEquals(14, resultDateTime.hour)
            assertEquals(30, resultDateTime.minute)
        }
    }
}
