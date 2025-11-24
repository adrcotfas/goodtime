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
package com.apps.adrcotfas.goodtime.stats.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.TimeUtils.getLocalizedDayNamesForStats
import com.apps.adrcotfas.goodtime.bl.TimeUtils.getLocalizedMonthNamesForStats
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.stats.StatisticsHistoryViewModel
import com.apps.adrcotfas.goodtime.ui.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import com.patrykandpatrick.vico.multiplatform.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.Scroll
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.columnSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.ProvideVicoTheme
import com.patrykandpatrick.vico.multiplatform.common.component.rememberLineComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.multiplatform.m3.common.rememberM3VicoTheme
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_default_label_name
import goodtime_productivity.composeapp.generated.resources.labels_others
import goodtime_productivity.composeapp.generated.resources.stats_history_interval_type_options
import goodtime_productivity.composeapp.generated.resources.stats_history_title
import goodtime_productivity.composeapp.generated.resources.stats_total
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.minutes

@Composable
fun HistorySection(viewModel: StatisticsHistoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.data.x.isEmpty() || uiState.data.y.isEmpty()) return

    val data = uiState.data
    val type = uiState.type
    val isTimeOverviewType = uiState.overviewType == OverviewType.TIME

    val isLineChart = uiState.isLineChart

    val primaryColor = MaterialTheme.colorScheme.primary

    val colors =
        uiState.selectedLabels
            .map {
                MaterialTheme.getLabelColor(it.colorIndex)
            }.plus(MaterialTheme.getLabelColor(Label.OTHERS_LABEL_COLOR_INDEX.toLong()))

    val x = remember(data) { data.x }
    val y = remember(data) { data.y }

    val modelProducer = remember(isLineChart) { CartesianChartModelProducer() }

    val monthNames = remember { getLocalizedMonthNamesForStats() }
    val dayNames = remember { getLocalizedDayNamesForStats() }

    val bottomAxisStrings =
        remember(monthNames, dayNames) {
            BottomAxisStrings(
                dayOfWeekNames = DayOfWeek.entries.map { dayNames[it.ordinal].take(3) },
                monthsOfYearNames = Month.entries.map { monthNames[it.ordinal].take(3) },
            )
        }

    LaunchedEffect(data, isLineChart) {
        if (isLineChart) {
            modelProducer.runTransaction {
                lineSeries {
                    series(y[Label.DEFAULT_LABEL_NAME]!!)
                    extras { it[timestampsKey] = x }
                    extras { it[labelsKey] = y.keys }
                    extras { it[extraBottomAxisStrings] = bottomAxisStrings }
                    extras { it[extraViewType] = type }
                    extras { it[extraFirstDayOfWeek] = uiState.firstDayOfWeek }
                }
            }
        } else {
            modelProducer.runTransaction {
                columnSeries { y.values.forEach { series(it) } }
                extras { it[timestampsKey] = x }
                extras { it[labelsKey] = y.keys }
                extras { it[extraBottomAxisStrings] = bottomAxisStrings }
                extras { it[extraViewType] = type }
                extras { it[extraFirstDayOfWeek] = uiState.firstDayOfWeek }
            }
        }
    }

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
                stringResource(Res.string.stats_history_title),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = primaryColor,
                    ),
            )

            DropdownMenuBox(
                textStyle = MaterialTheme.typography.bodySmall,
                value = stringArrayResource(Res.array.stats_history_interval_type_options)[type.ordinal],
                options = stringArrayResource(Res.array.stats_history_interval_type_options).toList(),
                onDismissRequest = {},
                onDropdownMenuItemSelected = {
                    viewModel.setType(HistoryIntervalType.entries[it])
                },
            )
        }

        val modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 32.dp, top = 16.dp, bottom = 16.dp)
        if (isLineChart) {
            LineHistoryChart(
                modifier = modifier,
                modelProducer = modelProducer,
                isTimeOverviewType = isTimeOverviewType,
                primaryColor = primaryColor,
            )
        } else {
            BarHistoryChart(
                modifier = modifier,
                modelProducer = modelProducer,
                isTimeOverviewType = isTimeOverviewType,
                colors = colors,
            )
        }
    }
}

private val timeStartAxisValueFormatter =
    CartesianValueFormatter { _, value, _ ->
        value.minutes.formatOverview()
    }
private val timeStartAxisItemPlacer = VerticalAxis.ItemPlacer.step({ 30.0 })
private val sessionsStartAxisItemPlacer = VerticalAxis.ItemPlacer.step({ 5.0 })

@Composable
private fun BarHistoryChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    isTimeOverviewType: Boolean,
    colors: List<Color>,
) {
    val defaultLabelName = stringResource(Res.string.labels_default_label_name)
    val othersLabelName = stringResource(Res.string.labels_others)
    val totalLabel = stringResource(Res.string.stats_total)
    val othersLabelColor = colors.last()

    val scrollState =
        rememberVicoScrollState(
            scrollEnabled = true,
            initialScroll = Scroll.Absolute.End,
            autoScrollCondition = AutoScrollCondition.OnModelGrowth,
        )

    val markerValueFormatter =
        HistoryBarChartMarkerValueFormatter(
            defaultLabelName = defaultLabelName,
            othersLabelName = othersLabelName,
            othersLabelColor = othersLabelColor,
            isTimeOverviewType = isTimeOverviewType,
            totalLabel = totalLabel,
        )

    ProvideVicoTheme(
        rememberM3VicoTheme(
            lineColor =
                MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.5f,
                ),
        ),
    ) {
        CartesianChartHost(
            modifier = modifier.height(200.dp),
            scrollState = scrollState,
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            chart =
                rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider =
                            ColumnCartesianLayer.ColumnProvider.series(
                                colors.mapIndexed { _, color ->
                                    rememberLineComponent(
                                        fill = Fill(color),
                                        thickness = 12.dp,
                                    )
                                },
                            ),
                        columnCollectionSpacing = 24.dp,
                        mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            label =
                                rememberAxisLabelComponent(
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                ),
                            valueFormatter = if (isTimeOverviewType) timeStartAxisValueFormatter else CartesianValueFormatter.decimal(),
                            itemPlacer = if (isTimeOverviewType) timeStartAxisItemPlacer else sessionsStartAxisItemPlacer,
                        ),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            guideline = null,
                            label =
                                rememberAxisLabelComponent(
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    lineCount = 2,
                                ),
                            valueFormatter = BottomAxisValueFormatter,
                            itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                        ),
                    marker = rememberMarker(markerValueFormatter),
                ),
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun LineHistoryChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    isTimeOverviewType: Boolean,
    primaryColor: Color,
) {
    val scrollState =
        rememberVicoScrollState(
            scrollEnabled = true,
            initialScroll = Scroll.Absolute.End,
            autoScrollCondition = AutoScrollCondition.OnModelGrowth,
        )

    val markerValueFormatter =
        HistoryLineChartMarkerValueFormatter(
            isTimeOverviewType = isTimeOverviewType,
        )

    ProvideVicoTheme(
        rememberM3VicoTheme(
            lineColor =
                MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.5f,
                ),
        ),
    ) {
        CartesianChartHost(
            modifier = modifier.height(200.dp),
            scrollState = scrollState,
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            chart =
                rememberCartesianChart(
                    rememberLineCartesianLayer(
                        pointSpacing = 36.dp,
                        lineProvider =
                            LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(primaryColor)),
                                    areaFill =
                                        LineCartesianLayer.AreaFill.single(
                                            Fill(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        primaryColor.copy(alpha = 0.3f),
                                                        primaryColor.copy(alpha = 0.1f),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    pointProvider =
                                        LineCartesianLayer.PointProvider.single(
                                            LineCartesianLayer.Point(
                                                size = 6.dp,
                                                component =
                                                    rememberShapeComponent(
                                                        Fill(primaryColor),
                                                        shape = RoundedCornerShape(4.dp),
                                                    ),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            label =
                                rememberAxisLabelComponent(
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                ),
                            valueFormatter = if (isTimeOverviewType) timeStartAxisValueFormatter else CartesianValueFormatter.decimal(),
                            itemPlacer = if (isTimeOverviewType) timeStartAxisItemPlacer else sessionsStartAxisItemPlacer,
                        ),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            guideline = null,
                            label =
                                rememberAxisLabelComponent(
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    lineCount = 2,
                                ),
                            valueFormatter = BottomAxisValueFormatter,
                        ),
                    marker = rememberMarker(markerValueFormatter),
                ),
            modelProducer = modelProducer,
        )
    }
}
