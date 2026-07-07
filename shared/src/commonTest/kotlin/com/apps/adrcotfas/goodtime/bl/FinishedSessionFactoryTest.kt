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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class FinishedSessionFactoryTest {
    private fun data(
        startTime: Long,
        endTime: Long,
        timeSpentPaused: Long = 0,
        type: TimerType = TimerType.FOCUS,
    ) = DomainTimerData(
        isReady = true,
        runtime =
            TimerRuntimeState(
                startTime = startTime,
                endTime = endTime,
                timeSpentPaused = timeSpentPaused,
                type = type,
                state = TimerState.FINISHED,
            ),
    )

    @Test
    fun `sessions shorter than a minute are dropped`() {
        assertNull(
            FinishedSessionFactory.create(
                data = data(startTime = 0, endTime = 30_000),
                now = 1_000_000,
                elapsedRealtime = 30_000,
            ),
        )
    }

    @Test
    fun `focus session subtracts interruptions and converts to wall clock`() {
        val result =
            FinishedSessionFactory.create(
                data =
                    data(
                        startTime = 0,
                        endTime = 25.minutes.inWholeMilliseconds,
                        timeSpentPaused = 5.minutes.inWholeMilliseconds,
                    ),
                now = 2_000_000,
                elapsedRealtime = 25.minutes.inWholeMilliseconds,
                notes = "note",
            )

        assertNotNull(result)
        val (session, minutes) = result
        assertEquals(20L, minutes) // 25 focus - 5 paused (+ wiggle room rounding)
        assertEquals(20L, session.duration)
        assertEquals(5L, session.interruptions)
        // wall clock at end = now - elapsedRealtime + endTime = now
        assertEquals(2_000_000, session.timestamp)
        assertEquals("note", session.notes)
    }

    @Test
    fun `break session keeps interruptions out and full duration`() {
        val result =
            FinishedSessionFactory.create(
                data =
                    data(
                        startTime = 0,
                        endTime = 10.minutes.inWholeMilliseconds,
                        timeSpentPaused = 2.minutes.inWholeMilliseconds,
                        type = TimerType.BREAK,
                    ),
                now = 3_000_000,
                elapsedRealtime = 10.minutes.inWholeMilliseconds,
            )

        assertNotNull(result)
        val (session, minutes) = result
        assertEquals(10L, minutes)
        assertEquals(0L, session.interruptions)
        assertEquals(false, session.isWork)
    }
}
