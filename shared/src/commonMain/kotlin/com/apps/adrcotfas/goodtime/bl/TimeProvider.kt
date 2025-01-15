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

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface TimeProvider {
    /**
     * Returns the current time in milliseconds since Unix Epoch.
     */
    fun now(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    /**
     * Returns the current time in milliseconds since boot, including time spent in sleep.
     */
    fun elapsedRealtime(): Long

    fun startOfTodayAdjusted(dayStartSecondOfDay: Int): Long {
        val currentInstant = Instant.fromEpochMilliseconds(now())
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = currentInstant.toLocalDateTime(timeZone)
        val startOfDay = currentDateTime.date.atTime(LocalTime.fromSecondOfDay(dayStartSecondOfDay))
        return startOfDay.toInstant(timeZone).toEpochMilliseconds()
    }

    fun startOfThisWeekAdjusted(startDayOfWeek: DayOfWeek, dayStartSecondOfDay: Int): Long {
        val currentInstant = Instant.fromEpochMilliseconds(now())
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = currentInstant.toLocalDateTime(timeZone)
        var date = currentDateTime.date

        while (date.dayOfWeek != startDayOfWeek) {
            date = date.minus(1, DateTimeUnit.DAY)
        }

        val startOfWeek = date.atTime(LocalTime.fromSecondOfDay(dayStartSecondOfDay))
        return startOfWeek.toInstant(timeZone).toEpochMilliseconds()
    }

    fun startOfThisMonthAdjusted(dayStartSecondOfDay: Int): Long {
        val currentInstant = Instant.fromEpochMilliseconds(now())
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = currentInstant.toLocalDateTime(timeZone)

        val date = LocalDate(
            currentDateTime.date.year,
            currentDateTime.date.month,
            1,
        )
        val startOfMonth = date.atTime(LocalTime.fromSecondOfDay(dayStartSecondOfDay))
        return startOfMonth.toInstant(timeZone).toEpochMilliseconds()
    }
}

expect fun createTimeProvider(): TimeProvider
