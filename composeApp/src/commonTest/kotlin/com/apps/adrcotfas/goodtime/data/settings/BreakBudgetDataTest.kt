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
package com.apps.adrcotfas.goodtime.data.settings

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class BreakBudgetDataTest {
    @Test
    fun `Compute remaining time budget`() =
        runTest {
            val persistedTimerData =
                BreakBudgetData(
                    breakBudgetStart = 0,
                    breakBudget = 10.minutes,
                )

            val expected =
                mapOf(
                    Pair(10.minutes, 0.milliseconds.toLong(DurationUnit.MILLISECONDS)),
                    Pair(9.minutes, 1.minutes.toLong(DurationUnit.MILLISECONDS)),
                    Pair(8.minutes, 89.seconds.toLong(DurationUnit.MILLISECONDS)),
                    Pair(8.minutes, 90.seconds.toLong(DurationUnit.MILLISECONDS)),
                    Pair(8.minutes, 91.seconds.toLong(DurationUnit.MILLISECONDS)),
                    Pair(0.minutes, 9.minutes.plus(1.seconds).toLong(DurationUnit.MILLISECONDS)),
                    Pair(0.minutes, 10.minutes.toLong(DurationUnit.MILLISECONDS)),
                    Pair(0.minutes, 10.minutes.plus(1.seconds).toLong(DurationUnit.MILLISECONDS)),
                )

            for ((budget, elapsedTime) in expected) {
                assertEquals(
                    budget.inWholeMinutes,
                    persistedTimerData.getRemainingBreakBudget(elapsedTime).inWholeMinutes,
                )
            }
        }
}
