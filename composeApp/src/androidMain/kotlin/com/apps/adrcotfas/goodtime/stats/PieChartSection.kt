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

import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.ui.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_default_label_name
import goodtime_productivity.composeapp.generated.resources.labels_others
import goodtime_productivity.composeapp.generated.resources.stats_focus_distribution
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.minutes

@Composable
actual fun PieChartSection(
    overviewData: SessionOverviewData,
    overviewDurationType: OverviewDurationType,
    onChangeType: (OverviewDurationType) -> Unit,
    typeNames: Map<OverviewDurationType, String>,
    selectedLabels: List<LabelData>,
    color: Color,
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()

    val workPerLabel =
        when (overviewDurationType) {
            OverviewDurationType.TODAY -> overviewData.workTodayPerLabel.filterValues { it != 0L }
            OverviewDurationType.THIS_WEEK -> overviewData.workThisWeekPerLabel.filterValues { it != 0L }
            OverviewDurationType.THIS_MONTH -> overviewData.workThisMonthPerLabel.filterValues { it != 0L }
            OverviewDurationType.TOTAL -> overviewData.workTotalPerLabel.filterValues { it != 0L }
        }

    val entries = ArrayList<PieEntry>()
    for (label in workPerLabel.keys) {
        val labelName =
            when (label) {
                Label.DEFAULT_LABEL_NAME -> stringResource(Res.string.labels_default_label_name)
                Label.OTHERS_LABEL_NAME -> stringResource(Res.string.labels_others)
                else -> label
            }
        entries.add(PieEntry(workPerLabel[label]!!.toFloat(), labelName))
    }

    // Might happen when we toggle showing archived labels
    if (!selectedLabels
            .map { it.name }
            .containsAll(workPerLabel.keys.minus(Label.OTHERS_LABEL_NAME))
    ) {
        return
    }

    val colors =
        workPerLabel.keys
            .map {
                when (it) {
                    Label.OTHERS_LABEL_NAME -> {
                        MaterialTheme.getLabelColor(Label.OTHERS_LABEL_COLOR_INDEX.toLong())
                    }

                    else -> {
                        MaterialTheme.getLabelColor(
                            selectedLabels.first { label -> label.name == it }.colorIndex,
                        )
                    }
                }
            }.map { it.toArgb() }

    Utils.init(context)
    val dataSet =
        PieDataSet(entries, "").apply {
            this.colors = colors
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.175f
            sliceSpace = 4f
            setUseValueColorForLine(true)
        }

    val data = PieData(dataSet)

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
                stringResource(Res.string.stats_focus_distribution),
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
                    .height(224.dp)
                    .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center,
        ) {
            if (entries.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = "No items",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            } else {
                var showPercentages by rememberSaveable { mutableStateOf(true) }
                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .align(Alignment.CenterHorizontally),
                    factory = { context ->
                        PieChart(context).apply {
                            isDrawHoleEnabled = true
                            holeRadius = 80f
                            transparentCircleRadius = 0f
                            dragDecelerationFrictionCoef = 0.95f
                            isRotationEnabled = true
                            isHighlightPerTapEnabled = false
                            setHoleColor(backgroundColor)
                            legend.isEnabled = false
                            description.isEnabled = false
                            setUsePercentValues(true)
                            setExtraOffsets(8f, 8f, 8f, 8f)
                            highlightValues(null)
                            setEntryLabelColor(onBackgroundColor)
                            setEntryLabelTextSize(12f)
                            setNoDataText("")
                            setTouchEnabled(true)
                            onChartGestureListener =
                                object : PieChartGestureListener() {
                                    override fun onChartSingleTapped(me: MotionEvent) {
                                        showPercentages = !showPercentages
                                    }
                                }
                        }
                    },
                ) { chart ->

                    chart.setDrawSlicesUnderHole(true)
                    chart.setEntryLabelColor(onBackgroundColor)

                    chart.invalidate()
                    chart.data =
                        data.apply {
                            setValueTextSize(12f)
                            setValueTextColor(onBackgroundColor)
                        }
                    if (showPercentages) {
                        chart.setUsePercentValues(true)
                        data.setValueFormatter(PercentFormatter())
                    } else {
                        chart.setUsePercentValues(false)
                        data.setValueFormatter(
                            object : IValueFormatter {
                                override fun getFormattedValue(
                                    value: Float,
                                    entry: Entry?,
                                    dataSetIndex: Int,
                                    viewPortHandler: ViewPortHandler?,
                                ): String = value.toLong().minutes.formatOverview()
                            },
                        )
                    }
                }
            }
        }
    }
}

open class PieChartGestureListener : OnChartGestureListener {
    override fun onChartSingleTapped(me: MotionEvent) {}

    override fun onChartGestureStart(
        me: MotionEvent,
        lastPerformedGesture: ChartGesture,
    ) {
    }

    override fun onChartGestureEnd(
        me: MotionEvent,
        lastPerformedGesture: ChartGesture,
    ) {
    }

    override fun onChartLongPressed(me: MotionEvent) {}

    override fun onChartDoubleTapped(me: MotionEvent) {}

    override fun onChartFling(
        me1: MotionEvent,
        me2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ) {
    }

    override fun onChartScale(
        me: MotionEvent,
        scaleX: Float,
        scaleY: Float,
    ) {
    }

    override fun onChartTranslate(
        me: MotionEvent,
        dX: Float,
        dY: Float,
    ) {
    }
}
