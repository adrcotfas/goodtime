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

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DurationExtensionTest {
    @Test
    fun testFormatMinutes() {
        assertEquals(
            1
                .days
                .plus(2.hours)
                .plus(30.minutes)
                .formatOverview(),
            "26h 30min",
        )
        assertEquals(
            40
                .days
                .plus(2.hours)
                .plus(30.minutes)
                .formatOverview(),
            "962h 30min",
        )
        assertEquals(99999.hours.plus(30.minutes).formatOverview(), "99999h 30min")
    }

    @Test
    fun `endOfWeekInThisWeek is the last day of the week containing this date`() {
        // 2026-07-20 is a Monday
        val monday = LocalDate.parse("2026-07-20")
        for (offset in 0..6) {
            val date = monday.plus(DatePeriod(days = offset))
            assertEquals(
                LocalDate.parse("2026-07-26"),
                date.endOfWeekInThisWeek(DayOfWeek.MONDAY),
                "failed for $date",
            )
        }
        assertEquals(
            LocalDate.parse("2026-07-25"),
            monday.endOfWeekInThisWeek(DayOfWeek.SUNDAY),
        )
    }

    /** The heatmap grid renders whole weeks, so the range must always be a multiple of 7 days. */
    @Test
    fun `the heatmap year spans a whole number of weeks`() {
        val monday = LocalDate.parse("2026-07-20")
        for (firstDayOfWeek in DayOfWeek.entries) {
            for (offset in 0..6) {
                val today = monday.plus(DatePeriod(days = offset))
                val start = today.minus(DatePeriod(days = 363)).firstDayOfWeekInThisWeek(firstDayOfWeek)
                val end = today.endOfWeekInThisWeek(firstDayOfWeek)
                assertEquals(
                    0,
                    (start.daysUntil(end) + 1) % 7,
                    "failed for $today, week starting on $firstDayOfWeek",
                )
            }
        }
    }
}
