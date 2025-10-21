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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.AndroidTimeUtils.localizedMonthNamesFull
import com.apps.adrcotfas.goodtime.common.Time.currentDateTime
import com.apps.adrcotfas.goodtime.common.isoWeekNumber
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.data.settings.StatisticsSettings
import com.apps.adrcotfas.goodtime.stats.history.HistorySection
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.stats_today
import goodtime_productivity.composeapp.generated.resources.stats_total
import goodtime_productivity.composeapp.generated.resources.stats_week
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Composable
fun OverviewTab(
    firstDayOfWeek: DayOfWeek,
    workDayStart: Int,
    statisticsSettings: StatisticsSettings,
    statisticsData: StatisticsData,
    onChangeOverviewType: (OverviewType) -> Unit,
    onChangeOverviewDurationType: (OverviewDurationType) -> Unit,
    onChangePieChartOverviewType: (OverviewDurationType) -> Unit,
    historyChartViewModel: StatisticsHistoryViewModel,
) {
    val locale = androidx.compose.ui.text.intl.Locale.current
    val javaLocale = remember(locale) { Locale.forLanguageTag(locale.toLanguageTag()) }

    val currentDateTime = remember { currentDateTime() }
    val uiState by historyChartViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        val typeNames =
            mapOf(
                OverviewDurationType.TODAY to stringResource(Res.string.stats_today),
                OverviewDurationType.THIS_WEEK to
                    stringResource(
                        Res.string.stats_week,
                        currentDateTime.date.isoWeekNumber(),
                    ),
                OverviewDurationType.THIS_MONTH to localizedMonthNamesFull(javaLocale)[currentDateTime.month.ordinal],
                OverviewDurationType.TOTAL to stringResource(Res.string.stats_total),
            )

        OverviewSection(
            statisticsData.overviewData,
            typeNames,
            statisticsSettings.overviewType,
            onChangeOverviewType,
        )

        HistorySection(historyChartViewModel)

        ProductiveTimeSection(
            statisticsData.productiveHoursOfTheDay,
            workDayStart,
        )

        HeatmapSection(
            firstDayOfWeek,
            data = statisticsData.heatmapData,
        )

        if (uiState.selectedLabels.size > 1) {
            PieChartSection(
                statisticsData.overviewData,
                statisticsSettings.pieChartViewType,
                onChangePieChartOverviewType,
                typeNames = typeNames,
                selectedLabels = uiState.selectedLabels,
            )
        }

        WorkBreakRatioSection(
            statisticsData.overviewData,
            statisticsSettings.overviewDurationType,
            onChangeOverviewDurationType,
            typeNames = typeNames,
        )
    }
}
