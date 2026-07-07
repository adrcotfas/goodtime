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
package com.apps.adrcotfas.goodtime.stats

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class StatisticsDataTest {
    @Test
    fun calculateSessionWeightTest() {
        val todayTimestamp = 641131200000L

        // Test case: session from today
        val sessionToday = todayTimestamp
        assertEquals(1f, calculateSessionWeight(sessionToday, todayTimestamp), 0.015f)

        // Test case: session from 12 months ago
        val session12MonthsAgo = todayTimestamp - 365.days.inWholeMilliseconds
        assertEquals(0f, calculateSessionWeight(session12MonthsAgo, todayTimestamp), 0.015f)

        // Test case: session from 9 months ago
        val session9MonthsAgo = todayTimestamp - 9 * 30.days.inWholeMilliseconds
        assertEquals(0.25f, calculateSessionWeight(session9MonthsAgo, todayTimestamp), 0.015f)

        // Test case: session from 6 months ago
        val session6MonthsAgo = todayTimestamp - 6 * 30.days.inWholeMilliseconds
        assertEquals(0.5f, calculateSessionWeight(session6MonthsAgo, todayTimestamp), 0.015f)

        // Test case: session from 3 months ago
        val session3MonthsAgo = todayTimestamp - 3 * 30.days.inWholeMilliseconds
        assertEquals(0.75f, calculateSessionWeight(session3MonthsAgo, todayTimestamp), 0.015f)
    }

    @Test
    fun testIntervals() {
        val localDate = LocalDate(2025, 1, 1)
        val endOfMonth = localDate.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        assertEquals(endOfMonth, LocalDate(2025, 1, 31))

        val endOfNextMonth = endOfMonth.plus(DatePeriod(months = 1))
        assertEquals(endOfNextMonth, LocalDate(2025, 2, 28))
    }

    @Test
    fun testSplitSessionByHourSingleHourSession() {
        val endDateTime = LocalDateTime(2025, 12, 1, 12, 35)
        val duration = 35L
        val expected = mapOf(12 to 35L)
        assertEquals(expected, splitSessionByHour(endDateTime, duration))
    }

    @Test
    fun testSplitSessionByHourTwoHourSession() {
        val endDateTime = LocalDateTime(2025, 12, 1, 12, 20)
        val duration = 62L
        val expected = mapOf(11 to 42L, 12 to 20L)
        assertEquals(expected, splitSessionByHour(endDateTime, duration))
    }

    @Test
    fun testSplitSessionByHourCrossMidnightSession() {
        val endDateTime = LocalDateTime(2025, 12, 1, 0, 30)
        val duration = 67L
        val expected = mapOf(23 to 37L, 0 to 30L)
        assertEquals(expected, splitSessionByHour(endDateTime, duration))
    }

    @Test
    fun testSplitSessionByHourLongSession() {
        val endDateTime = LocalDateTime(2025, 12, 1, 9, 0)
        val duration = 150L
        val expected = mapOf(6 to 30L, 7 to 60L, 8 to 60L)
        assertEquals(expected, splitSessionByHour(endDateTime, duration))
    }
}
