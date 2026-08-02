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

import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatMilliseconds
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeUtilsTest {
    @Test
    fun formatMillisecondsUnderOneHour() {
        assertEquals("00:00", 0L.formatMilliseconds())
        assertEquals("05:42", (5 * 60_000L + 42_000).formatMilliseconds())
        assertEquals("59:59", (59 * 60_000L + 59_000).formatMilliseconds())
    }

    @Test
    fun formatMillisecondsRollsOverToHours() {
        assertEquals("1:00:00", (60 * 60_000L).formatMilliseconds())
        assertEquals("3:18:26", (198 * 60_000L + 26_000).formatMilliseconds())
    }

    @Test
    fun formatMillisecondsMinutesOnly() {
        assertEquals("06", (5 * 60_000L + 42_000).formatMilliseconds(minutesOnly = true))
        assertEquals("3:19", (198 * 60_000L + 26_000).formatMilliseconds(minutesOnly = true))
    }
}
