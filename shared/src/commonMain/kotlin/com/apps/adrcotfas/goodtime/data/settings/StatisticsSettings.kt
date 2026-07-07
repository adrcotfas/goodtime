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

enum class OverviewType {
    SESSIONS,
    TIME,
}

enum class OverviewDurationType {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    TOTAL,
}

enum class HistoryIntervalType {
    DAYS,
    WEEKS,
    MONTHS,
    QUARTERS,
    YEARS,
}

@Serializable
data class StatisticsSettings(
    val overviewType: OverviewType = OverviewType.TIME,
    val overviewDurationType: OverviewDurationType = OverviewDurationType.THIS_MONTH,
    val pieChartViewType: OverviewDurationType = OverviewDurationType.THIS_MONTH,
    val showBreaks: Boolean = false,
    val showArchived: Boolean = false,
)

@Serializable
data class HistoryChartSettings(
    val intervalType: HistoryIntervalType = HistoryIntervalType.DAYS,
    val isLineChart: Boolean = true,
)
