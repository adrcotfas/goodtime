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

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BreakBudgetData(
    val breakBudget: Duration = 0.minutes,
    val breakBudgetStart: Long = 0, // millis since boot
    val isAccumulating: Boolean = false,
) {
    fun getRemainingBreakBudget(elapsedRealtime: Long): Duration {
        val timeSinceBreakBudgetStart = elapsedRealtime - breakBudgetStart
        val breakBudgetMs = breakBudget.inWholeMilliseconds
        return max(0, (breakBudgetMs - timeSinceBreakBudgetStart)).milliseconds
    }
}

@Serializable
data class LongBreakData(
    val streak: Int = 0,
    val lastWorkEndTime: Long = 0, // millis since boot
)

fun LongBreakData.streakInUse(sessionsBeforeLongBreak: Int): Int = streak % sessionsBeforeLongBreak
