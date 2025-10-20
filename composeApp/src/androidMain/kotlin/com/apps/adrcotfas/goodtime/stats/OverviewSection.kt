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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuBox
import kotlin.time.Duration.Companion.minutes

@Composable
fun OverviewSection(
    data: SessionOverviewData,
    typeNames: Map<OverviewDurationType, String>,
    type: OverviewType,
    onTypeChanged: (OverviewType) -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
                text = stringResource(R.string.stats_overview),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = color,
                    ),
            )
            DropdownMenuBox(
                textStyle = MaterialTheme.typography.bodySmall,
                value = stringArrayResource(R.array.stats_overview_type_options)[type.ordinal],
                options = stringArrayResource(R.array.stats_overview_type_options).toList(),
                onDismissRequest = {},
                onDropdownMenuItemSelected = {
                    onTypeChanged(OverviewType.entries[it])
                },
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            OverviewDurationType.entries.forEach { overviewType ->
                val valueWork =
                    if (type == OverviewType.TIME) {
                        when (overviewType) {
                            OverviewDurationType.TODAY -> data.workToday
                            OverviewDurationType.THIS_WEEK -> data.workThisWeek
                            OverviewDurationType.THIS_MONTH -> data.workThisMonth
                            OverviewDurationType.TOTAL -> data.workTotal
                        }.minutes.formatOverview()
                    } else {
                        when (overviewType) {
                            OverviewDurationType.TODAY -> data.workSessionsToday
                            OverviewDurationType.THIS_WEEK -> data.workSessionsThisWeek
                            OverviewDurationType.THIS_MONTH -> data.workSessionsThisMonth
                            OverviewDurationType.TOTAL -> data.workSessionsTotal
                        }.toString()
                    }

                OverviewTypeSection(
                    modifier = Modifier.weight(1f / OverviewDurationType.entries.size),
                    title = typeNames[overviewType]!!,
                    valueWork = valueWork,
                    colorWork = color,
                )
            }
        }
    }
}

@Composable
fun OverviewTypeSection(
    modifier: Modifier = Modifier,
    title: String,
    valueWork: String,
    colorWork: Color,
) {
    Column(
        modifier = modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            valueWork,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    color = colorWork,
                    textAlign = TextAlign.Center,
                ),
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
