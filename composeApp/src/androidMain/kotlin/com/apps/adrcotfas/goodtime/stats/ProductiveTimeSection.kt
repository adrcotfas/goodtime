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

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.convertSpToDp
import kotlinx.datetime.LocalTime

private fun Int.toFormattedHour(is24HourFormat: Boolean): String =
    if (is24HourFormat) {
        "$this"
    } else {
        val hour =
            if (this == 0) {
                12
            } else if (this > 12) {
                this - 12
            } else {
                this
            }
        val amPm = if (this < 12) "AM" else "PM"
        "$hour\n$amPm"
    }

fun <K, V> Map<K, V>.rotate(startKey: K): Map<K, V> {
    val startIndex = this.keys.indexOf(startKey)
    if (startIndex == -1) return this // If the startKey is not found, return the original map

    val firstPart = this.entries.drop(startIndex).associate { it.toPair() }
    val secondPart = this.entries.take(startIndex).associate { it.toPair() }

    return firstPart + secondPart
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductiveTimeSection(
    productiveHoursOfTheDay: ProductiveHoursOfTheDay,
    workDayStart: Int,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }

    val workdayStartHour = LocalTime.fromSecondOfDay(workDayStart).hour
    val sortedData = productiveHoursOfTheDay.rotate(workdayStartHour)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            text = stringResource(R.string.stats_productive_hours),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = color,
                ),
        )
        val density = LocalDensity.current
        val cellSize = remember { (convertSpToDp(density, 12.sp.value) * 1.5f).dp }
        val cellSpacing = remember { cellSize / 6f }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = cellSize,
                        end = 32.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompositionLocalProvider(
                LocalOverscrollFactory provides null,
            ) {
                val listState = rememberLazyListState()

                Row {
                    // the only reason for this is to have it aligned to the Heatmap section
                    Text(
                        modifier =
                            Modifier
                                .alpha(0f)
                                .padding(cellSpacing),
                        text = "mmm",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    LazyRow(
                        modifier = Modifier.wrapContentHeight(),
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
                    ) {
                        items(sortedData.toList(), key = { it.first }) { (hour, value) ->
                            Column(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
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
                                                        alpha = value,
                                                    ),
                                                ),
                                    )
                                }
                                if (hour % 3 == 0) {
                                    Text(
                                        modifier = Modifier.width(cellSize),
                                        text = hour.toFormattedHour(is24HourFormat),
                                        maxLines = 2,
                                        style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
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
