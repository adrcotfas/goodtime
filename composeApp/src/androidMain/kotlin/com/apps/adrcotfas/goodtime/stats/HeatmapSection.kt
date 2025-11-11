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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.bl.TimeUtils.getLocalizedDayNamesForStats
import com.apps.adrcotfas.goodtime.bl.TimeUtils.getLocalizedMonthNamesForStats
import com.apps.adrcotfas.goodtime.common.Time
import com.apps.adrcotfas.goodtime.common.at
import com.apps.adrcotfas.goodtime.common.convertSpToDp
import com.apps.adrcotfas.goodtime.common.endOfWeekInThisWeek
import com.apps.adrcotfas.goodtime.common.entriesStartingWithThis
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInMonth
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInThisWeek
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.stats_heatmap
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeatmapSection(
    firstDayOfWeek: DayOfWeek,
    data: HeatmapData,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val density = LocalDensity.current
    val endLocalDate = remember { Time.currentDateTime().date }
    val startLocalDate = remember { endLocalDate.minus(DatePeriod(days = 363)) }

    val startAtStartOfWeek = remember { startLocalDate.firstDayOfWeekInThisWeek(firstDayOfWeek) }
    val endAtEndOfWeek = remember { endLocalDate.endOfWeekInThisWeek(firstDayOfWeek) }
    val numberOfWeeks =
        remember {
            if (startLocalDate.daysUntil(endAtEndOfWeek) % 7 == 0) {
                52
            } else {
                53
            }
        }

    val fontSizeStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Thin)
    val cellSize = remember { (convertSpToDp(density, fontSizeStyle.fontSize.value) * 1.5f).dp }
    val cellSpacing = remember { cellSize / 6f }
    val daysInOrder = remember { firstDayOfWeek.entriesStartingWithThis() }

    val monthNames = remember { getLocalizedMonthNamesForStats() }
    val dayNames = remember { getLocalizedDayNamesForStats() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = numberOfWeeks - 1)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
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
                stringResource(Res.string.stats_heatmap),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = color,
                    ),
            )
        }

        CompositionLocalProvider(
            LocalOverscrollFactory provides null,
        ) {
            Row(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            top = 16.dp,
                            bottom = 16.dp,
                            start = cellSize,
                            end = 32.dp,
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(cellSize),
                    )
                    val labeledDays = mutableListOf(1, 3, 5)

                    daysInOrder.forEach {
                        if (labeledDays.contains(it.isoDayNumber)) {
                            Text(
                                modifier =
                                    Modifier
                                        .padding(cellSpacing)
                                        .height(cellSize),
                                text = dayNames[it.ordinal],
                                style = MaterialTheme.typography.labelSmall,
                            )
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(cellSpacing)
                                        .size(cellSize),
                            )
                        }
                    }
                }
                Column {
                    LazyRow(
                        modifier = Modifier.wrapContentHeight(),
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
                    ) {
                        items(numberOfWeeks) { index ->
                            Column(
                                modifier =
                                    Modifier
                                        .wrapContentHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val currentWeekStart =
                                    remember(index) { startAtStartOfWeek.plus(DatePeriod(days = index * 7)) }

                                val monthName = monthNames[currentWeekStart.month.ordinal]
                                if (currentWeekStart == startAtStartOfWeek ||
                                    currentWeekStart ==
                                    currentWeekStart.firstDayOfWeekInMonth(
                                        firstDayOfWeek,
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier.height(cellSize),
                                        text = monthName,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(cellSize))
                                }

                                daysInOrder.forEach { dayOfWeek ->
                                    val currentDay =
                                        remember(
                                            index,
                                            dayOfWeek,
                                        ) { currentWeekStart.at(dayOfWeek) }
                                    if (currentDay in startLocalDate..endLocalDate) {
                                        Box(modifier = Modifier.padding(cellSpacing)) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(cellSize)
                                                        .clip(MaterialTheme.shapes.extraSmall)
                                                        .background(
                                                            MaterialTheme.colorScheme.secondaryContainer.copy(
                                                                alpha = 0.5f,
                                                            ),
                                                        ),
                                            )
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(cellSize)
                                                        .clip(MaterialTheme.shapes.extraSmall)
                                                        .background(
                                                            color.copy(
                                                                alpha =
                                                                    data[currentDay]?.plus(0.2f)
                                                                        ?: 0f,
                                                            ),
                                                        ),
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .padding(cellSpacing)
                                                    .size(cellSize),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HeatmapSectionPreview() {
    HeatmapSection(
        firstDayOfWeek = DayOfWeek.MONDAY,
        data = emptyMap(),
    )
}
