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

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeTest {
    private val sixAm = LocalTime(6, 0).toSecondOfDay()

    private fun at(
        date: String,
        hour: Int,
        minute: Int = 0,
    ) = LocalDate.parse(date).let { LocalDateTime(it, LocalTime(hour, minute)) }

    @Test
    fun `startOfToday before the workday start belongs to the previous day`() {
        assertEquals(
            at("2026-07-24", 6),
            Time.toLocalDateTime(Time.startOfTodayAdjusted(sixAm, now = at("2026-07-25", 0, 30))),
        )
        assertEquals(
            at("2026-07-24", 6),
            Time.toLocalDateTime(Time.startOfTodayAdjusted(sixAm, now = at("2026-07-25", 5, 59))),
        )
    }

    @Test
    fun `startOfToday at or after the workday start belongs to the current day`() {
        assertEquals(
            at("2026-07-25", 6),
            Time.toLocalDateTime(Time.startOfTodayAdjusted(sixAm, now = at("2026-07-25", 6))),
        )
        assertEquals(
            at("2026-07-25", 6),
            Time.toLocalDateTime(Time.startOfTodayAdjusted(sixAm, now = at("2026-07-25", 23, 30))),
        )
    }

    @Test
    fun `startOfToday with a midnight workday start is the current midnight`() {
        assertEquals(
            at("2026-07-25", 0),
            Time.toLocalDateTime(Time.startOfTodayAdjusted(secondOfDay = 0, now = at("2026-07-25", 0, 30))),
        )
    }

    @Test
    fun `startOfThisWeek before the workday start on the first day belongs to the previous week`() {
        // 2026-07-20 is a Monday
        assertEquals(
            at("2026-07-13", 6),
            Time.startOfThisWeekAdjusted(DayOfWeek.MONDAY, sixAm, now = at("2026-07-20", 3)),
        )
        assertEquals(
            at("2026-07-20", 6),
            Time.startOfThisWeekAdjusted(DayOfWeek.MONDAY, sixAm, now = at("2026-07-20", 7)),
        )
    }

    @Test
    fun `startOfThisMonth before the workday start on the first day belongs to the previous month`() {
        assertEquals(
            at("2026-06-01", 6),
            Time.startOfThisMonth(sixAm, now = at("2026-07-01", 3)),
        )
        assertEquals(
            at("2026-07-01", 6),
            Time.startOfThisMonth(sixAm, now = at("2026-07-01", 9)),
        )
    }
}
