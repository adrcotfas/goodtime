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

import com.apps.adrcotfas.goodtime.data.model.Label
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryChartDataTest {
    @Test
    fun testAggregateDataIfNeeded() =
        runTest {
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
                ),
                mapOf<String, Long>(
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
}
