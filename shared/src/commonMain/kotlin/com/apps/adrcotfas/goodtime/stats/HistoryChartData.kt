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

import com.apps.adrcotfas.goodtime.common.Time.currentDateTime
import com.apps.adrcotfas.goodtime.common.Time.toLocalDateTime
import com.apps.adrcotfas.goodtime.common.firstDayOfThisQuarter
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInThisWeek
import com.apps.adrcotfas.goodtime.common.toEpochMilliseconds
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_DAYS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_MONTHS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_QUARTERS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_WEEKS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_YEARS
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.seconds

class HistoryChartData(
    val x: List<Long> = emptyList(),
    val y: Map<String, List<Long>> = emptyMap(),
) {
    companion object {
        const val NUM_DAYS = 365
        const val NUM_WEEKS = 105
        const val NUM_MONTHS = 24
        const val NUM_QUARTERS = 20
        const val NUM_YEARS = 10
    }
}

fun computeHistoryChartData(
    sessions: List<Session>,
    labels: List<String>,
    workDayStart: Int,
    firstDayOfWeek: DayOfWeek,
    type: HistoryIntervalType,
    overviewType: OverviewType,
    aggregate: Boolean,
): HistoryChartData {
    val today = currentDateTime().date

    data class IterationData(
        val intervalStart: LocalDate,
        val intervalLength: Int,
        val step: DatePeriod,
    )

    val iterationData: IterationData =
        when (type) {
            HistoryIntervalType.DAYS -> {
                IterationData(
                    intervalStart = today.minus(DatePeriod(days = NUM_DAYS)),
                    intervalLength = NUM_DAYS,
                    step = DatePeriod(days = 1),
                )
            }

            HistoryIntervalType.WEEKS -> {
                IterationData(
                    intervalStart =
                        today
                            .firstDayOfWeekInThisWeek(firstDayOfWeek)
                            .minus(DatePeriod(days = NUM_WEEKS * 7)),
                    intervalLength = NUM_WEEKS,
                    step = DatePeriod(days = 7),
                )
            }

            HistoryIntervalType.MONTHS -> {
                IterationData(
                    intervalStart =
                        LocalDate(
                            today.year,
                            today.month,
                            1,
                        ).minus(DatePeriod(months = NUM_MONTHS)),
                    intervalLength = NUM_MONTHS,
                    step = DatePeriod(months = 1),
                )
            }

            HistoryIntervalType.QUARTERS -> {
                IterationData(
                    intervalStart =
                        today
                            .firstDayOfThisQuarter()
                            .minus(DatePeriod(months = NUM_QUARTERS * 3)),
                    intervalLength = NUM_QUARTERS,
                    step = DatePeriod(months = 3),
                )
            }

            HistoryIntervalType.YEARS -> {
                IterationData(
                    intervalStart =
                        LocalDate(
                            today.year,
                            1,
                            1,
                        ).minus(DatePeriod(years = NUM_YEARS)),
                    intervalLength = NUM_YEARS,
                    step = DatePeriod(years = 1),
                )
            }
        }

    // timestamp to (label to duration)
    val intermediateData = mutableMapOf<LocalDate, Map<String, Long>>()

    // Initialize the data with zeros to default label
    var tmpDate = iterationData.intervalStart
    repeat(iterationData.intervalLength + 1) {
        intermediateData[tmpDate] =
            (if (aggregate) listOf(Label.DEFAULT_LABEL_NAME) else labels).associateWith { 0L }
        tmpDate = tmpDate.plus(iterationData.step)
    }

    sessions
        .asSequence()
        .map {
            val timestamp = it.timestamp
            PreProcessingSession(
                label = it.label,
                timestamp = timestamp,
                dateTime = toLocalDateTime(timestamp),
                adjustedDateTime = toLocalDateTime(timestamp - workDayStart.seconds.inWholeMilliseconds),
                duration = it.duration,
                isWork = it.isWork,
            )
        }.forEach { session ->
            val date = session.adjustedDateTime.date
            val label = if (aggregate) Label.DEFAULT_LABEL_NAME else session.label

            if (session.isWork) {
                val dateToConsider =
                    when (type) {
                        HistoryIntervalType.DAYS -> date
                        HistoryIntervalType.WEEKS -> date.firstDayOfWeekInThisWeek(firstDayOfWeek)
                        HistoryIntervalType.MONTHS -> LocalDate(date.year, date.month, 1)
                        HistoryIntervalType.QUARTERS -> date.firstDayOfThisQuarter()
                        HistoryIntervalType.YEARS -> LocalDate(date.year, 1, 1)
                    }

                if (dateToConsider in intermediateData.keys) {
                    val innerMap =
                        intermediateData
                            .getOrElse(dateToConsider) { mutableMapOf(label to 0L) }
                            .toMutableMap()
                    innerMap[label] =
                        innerMap.getOrElse(label) { 0L } + if (overviewType == OverviewType.TIME) session.duration else 1
                    intermediateData[dateToConsider] = innerMap
                }
            }
        }
    // Aggregate data if needed
    intermediateData.forEach {
        val aggregatedData = aggregateDataIfNeeded(it.value)
        intermediateData[it.key] = aggregatedData
    }

    // prepare data structures ready for the chart
    val x = mutableListOf<Long>()
    val y = mutableMapOf<String, List<Long>>()

    val emptyList = List(iterationData.intervalLength + 1) { 0L }
    intermediateData.asIterable().forEachIndexed { index, entry ->
        x.add(entry.key.toEpochMilliseconds())

        entry.value.forEach { (label, duration) ->
            y[label] =
                (y[label] ?: emptyList).toMutableList().apply {
                    this[index] = duration
                }
        }
    }

    return HistoryChartData(x = x, y = y)
}

/**
 * Aggregates the [data] if needed by grouping the small values according to [threshold] into an "Others" label.
 */
fun aggregateDataIfNeeded(
    data: Map<String, Long>,
    threshold: Double = 0.05,
    maxLabels: Int = 8,
): Map<String, Long> {
    if (data.isEmpty()) return data

    val totalValue = data.values.sum()
    if (totalValue == 0L) return data

    val smallKeys = data.filter { (_, v) -> v.toDouble() / totalValue < threshold }.keys
    val aggregatorNeeded = smallKeys.isNotEmpty() || data.size > maxLabels

    if (!aggregatorNeeded) return data

    val result = LinkedHashMap<String, Long>(data.size + 1)
    var othersSum = 0L

    data.forEach { (key, value) ->
        if (key in smallKeys) {
            othersSum += value
            result[key] = 0
        } else {
            result[key] = value
        }
    }

    if (othersSum > 0) {
        result[Label.OTHERS_LABEL_NAME] = othersSum
    }
    return result
}
