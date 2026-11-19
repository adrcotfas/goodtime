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
package com.apps.adrcotfas.goodtime.stats

import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class HistoryChartDataTest {
    @Test
    fun testAggregateDataIfNeeded() = runTest {
        assertEquals(
            aggregateDataIfNeeded(
                mapOf(
                    "1" to 1,
                    "2" to 1,
                    "3" to 1,
                    "4" to 1,
                    "5" to 1,
                    "6" to 1,
                    "7" to 1,
                    "8" to 1,
                    "9" to 1,
                    "10" to 1,
                    "11" to 1,
                ),
            ).filterValues { it > 0 },
            mapOf<String, Long>(
                "1" to 1,
                "2" to 1,
                "3" to 1,
                "4" to 1,
                "5" to 1,
                "6" to 1,
                "7" to 1,
                "8" to 1,
                Label.OTHERS_LABEL_NAME to 3,
            ),
        )

        assertEquals(
            aggregateDataIfNeeded(
                mapOf(
                    "1" to 15,
                    "2" to 15,
                    "3" to 15,
                    "4" to 15,
                    "5" to 15,
                    "6" to 15,
                    "7" to 5,
                    "8" to 1,
                    "9" to 1,
                    "10" to 1,
                    "11" to 1,
                    "12" to 1,
                ),
            ).filterValues { it > 0 },
            mapOf<String, Long>(
                "1" to 15,
                "2" to 15,
                "3" to 15,
                "4" to 15,
                "5" to 15,
                "6" to 15,
                "7" to 5,
                Label.OTHERS_LABEL_NAME to 5,
            ),
        )

        assertEquals(
            aggregateDataIfNeeded(
                mapOf(
                    "1" to 15,
                    "2" to 15,
                    "3" to 15,
                    "4" to 15,
                    "5" to 15,
                    "6" to 15,
                    "7" to 1,
                    "8" to 1,
                    "9" to 1,
                    "10" to 1,
                    "11" to 1,
                    "12" to 1,
                    "13" to 1,
                    "14" to 1,
                    "15" to 1,
                    "16" to 1,
                ),
            ).filterValues { it > 0 },
            mapOf<String, Long>(
                "1" to 15,
                "2" to 15,
                "3" to 15,
                "4" to 15,
                "5" to 15,
                "6" to 15,
                Label.OTHERS_LABEL_NAME to 10,
            ),
        )
    }

    @Test
    fun historyDaysPlacesSessionsInTheirBuckets() {
        val now = Clock.System.now().toEpochMilliseconds()
        val sessions =
            listOf(
                Session.create(timestamp = now, duration = 10, interruptions = 0, label = "a", isWork = true),
                Session.create(timestamp = now, duration = 5, interruptions = 0, label = "b", isWork = true),
                Session.create(timestamp = now - 1.days.inWholeMilliseconds, duration = 7, interruptions = 0, label = "a", isWork = true),
                Session.create(timestamp = now, duration = 99, interruptions = 0, label = "a", isWork = false),
            )
        val data =
            computeHistoryChartData(
                sessions = sessions,
                labels = listOf("a", "b"),
                workDayStart = 0,
                firstDayOfWeek = DayOfWeek.MONDAY,
                type = HistoryIntervalType.DAYS,
                overviewType = OverviewType.TIME,
                aggregate = false,
            )
        assertEquals(HistoryChartData.NUM_DAYS + 1, data.x.size)
        assertEquals(listOf(7L, 10L), data.y.getValue("a").takeLast(2))
        assertEquals(listOf(0L, 5L), data.y.getValue("b").takeLast(2))
        assertEquals(17L, data.y.getValue("a").sum())
    }
}
