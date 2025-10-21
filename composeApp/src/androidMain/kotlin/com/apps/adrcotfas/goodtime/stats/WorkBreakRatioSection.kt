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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuBox
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.stats_focus_break_ratio
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round
import kotlin.time.Duration.Companion.minutes

@Composable
fun WorkBreakRatioSection(
    overviewData: SessionOverviewData,
    overviewDurationType: OverviewDurationType,
    onChangeType: (OverviewDurationType) -> Unit,
    typeNames: Map<OverviewDurationType, String>,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val totalWork =
        when (overviewDurationType) {
            OverviewDurationType.TODAY -> overviewData.workToday
            OverviewDurationType.THIS_WEEK -> overviewData.workThisWeek
            OverviewDurationType.THIS_MONTH -> overviewData.workThisMonth
            OverviewDurationType.TOTAL -> overviewData.workTotal
        }
    val totalBreak =
        when (overviewDurationType) {
            OverviewDurationType.TODAY -> overviewData.breakToday
            OverviewDurationType.THIS_WEEK -> overviewData.breakThisWeek
            OverviewDurationType.THIS_MONTH -> overviewData.breakThisMonth
            OverviewDurationType.TOTAL -> overviewData.breakTotal
        }

    val denominator = totalWork + totalBreak
    val workPercentage = if (denominator > 0) round((totalWork.toDouble() * 100) / denominator) else 0.0
    val breakPercentage = if (denominator > 0) round(100 - workPercentage) else 0.0

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(Res.string.stats_focus_break_ratio),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = color,
                    ),
            )
            DropdownMenuBox(
                textStyle = MaterialTheme.typography.bodySmall,
                value = typeNames[overviewDurationType]!!,
                options = typeNames.values.toList(),
                onDismissRequest = {},
                onDropdownMenuItemSelected = {
                    onChangeType(OverviewDurationType.entries[it])
                },
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    totalWork.minutes.formatOverview(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = color),
                )
                Text(
                    totalBreak.minutes.formatOverview(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                progress = { workPercentage.toFloat() / 100 },
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    workPercentage.toInt().toString() + "%",
                    style = MaterialTheme.typography.bodyMedium.copy(color = color),
                )
                Text(
                    breakPercentage.toInt().toString() + "%",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
