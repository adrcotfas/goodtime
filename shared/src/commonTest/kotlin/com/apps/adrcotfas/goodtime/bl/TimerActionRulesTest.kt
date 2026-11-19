/**
 *     Goodtime Productivity
 *     Copyright (C) Adrian Cotfas
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

import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TimerActionRulesTest {
    private fun data(
        isCountdown: Boolean,
        type: TimerType,
        isBreakEnabled: Boolean = true,
    ) = DomainTimerData(
        label = DomainLabel(profile = TimerProfile(isCountdown = isCountdown, isBreakEnabled = isBreakEnabled, workBreakRatio = 3)),
        runtime = TimerRuntimeState(state = TimerState.RUNNING, type = type),
    )

    @Test
    fun `countdown sessions allow pause and +1 min`() {
        for (type in TimerType.entries) {
            val d = data(isCountdown = true, type = type)
            assertTrue(d.canPause())
            assertTrue(d.canAddMinute())
        }
    }

    @Test
    fun `count-up break cannot be paused or extended`() {
        val d = data(isCountdown = false, type = TimerType.BREAK)
        assertFalse(d.canPause())
        assertFalse(d.canAddMinute())
        assertTrue(d.canSkip(0))
    }

    @Test
    fun `count-up focus can skip only with at least one minute of break budget`() {
        val d = data(isCountdown = false, type = TimerType.FOCUS)
        assertTrue(d.canPause())
        assertFalse(d.canAddMinute())
        assertFalse(d.canSkip(2.minutes.inWholeMilliseconds))
        assertTrue(d.canSkip(3.minutes.inWholeMilliseconds))
    }

    @Test
    fun `skip is not offered when breaks are disabled`() {
        assertFalse(data(isCountdown = true, type = TimerType.FOCUS, isBreakEnabled = false).canSkip(0))
        assertFalse(data(isCountdown = false, type = TimerType.FOCUS, isBreakEnabled = false).canSkip(3.minutes.inWholeMilliseconds))
    }

    @Test
    fun `paused count-up focus spends the stored budget instead of earning more`() {
        val paused =
            data(isCountdown = false, type = TimerType.FOCUS).let {
                it.copy(
                    runtime = it.runtime.copy(state = TimerState.PAUSED),
                    breakBudgetData = BreakBudgetData(breakBudget = 5.minutes, breakBudgetStart = 0),
                )
            }
        assertEquals(5.minutes, paused.getBreakBudget(0))
        assertEquals(3.minutes, paused.getBreakBudget(2.minutes.inWholeMilliseconds))
        assertEquals(Duration.ZERO, paused.getBreakBudget(10.minutes.inWholeMilliseconds))
    }

    @Test
    fun `next is a break only after focus with breaks enabled`() {
        assertTrue(data(isCountdown = true, type = TimerType.FOCUS).nextIsBreak())
        assertFalse(data(isCountdown = true, type = TimerType.BREAK).nextIsBreak())
        assertFalse(data(isCountdown = true, type = TimerType.FOCUS, isBreakEnabled = false).nextIsBreak())
    }
}
