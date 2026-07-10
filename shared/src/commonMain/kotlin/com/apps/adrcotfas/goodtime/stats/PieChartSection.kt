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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.ui.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModel
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.labels_default_label_name
import goodtime_productivity.shared.generated.resources.labels_others
import goodtime_productivity.shared.generated.resources.stats_focus_distribution
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

@Composable
fun PieChartSection(
    overviewData: SessionOverviewData,
    overviewDurationType: OverviewDurationType,
    onChangeType: (OverviewDurationType) -> Unit,
    typeNames: Map<OverviewDurationType, String>,
    selectedLabels: List<LabelData>,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val workPerLabel =
        when (overviewDurationType) {
            OverviewDurationType.TODAY -> overviewData.workTodayPerLabel.filterValues { it != 0L }
            OverviewDurationType.THIS_WEEK -> overviewData.workThisWeekPerLabel.filterValues { it != 0L }
            OverviewDurationType.THIS_MONTH -> overviewData.workThisMonthPerLabel.filterValues { it != 0L }
            OverviewDurationType.TOTAL -> overviewData.workTotalPerLabel.filterValues { it != 0L }
        }

    // Might happen when we toggle showing archived labels
    if (!selectedLabels
            .map { it.name }
            .containsAll(workPerLabel.keys.minus(Label.OTHERS_LABEL_NAME))
    ) {
        return
    }

    val labels = workPerLabel.keys.toList()
    val values = labels.map { workPerLabel.getValue(it) }

    val defaultName = stringResource(Res.string.labels_default_label_name)
    val othersName = stringResource(Res.string.labels_others)
    val labelNames =
        labels.map {
            when (it) {
                Label.DEFAULT_LABEL_NAME -> defaultName
                Label.OTHERS_LABEL_NAME -> othersName
                else -> it
            }
        }
    val colors =
        labels.map {
            when (it) {
                Label.OTHERS_LABEL_NAME -> MaterialTheme.getLabelColor(Label.OTHERS_LABEL_COLOR_INDEX)
                else -> MaterialTheme.getLabelColor(selectedLabels.first { label -> label.name == it }.colorIndex)
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
            if (values.isEmpty()) {
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
                // TODO: revisit after https://github.com/patrykandpatrick/vico/pull/1548 is merged
                val rotation =
                    rememberSaveable(saver = Saver({ it.value }, { Animatable(it) })) { Animatable(0f) }
                val decay = rememberSplineBasedDecay<Float>()
                val scope = rememberCoroutineScope()
                val total = values.sum().toFloat()
                val onBackground = MaterialTheme.colorScheme.onBackground

                val labelComponent =
                    rememberTextComponent(
                        lineCount = 2,
                        style = TextStyle(color = onBackground, fontSize = 12.sp),
                    )
                val slices =
                    colors.map { sliceColor ->
                        PieChart.Slice(
                            fill = Fill(sliceColor),
                            label =
                            PieChart.SliceLabel.Outside(
                                textComponent = labelComponent,
                                lineColor = sliceColor,
                            ),
                        )
                    }
                val formatter =
                    PieValueFormatter { _, value, index ->
                        val name = labelNames[index]
                        if (showPercentages) {
                            "$name\n${(value / total * 100).roundToInt()}%"
                        } else {
                            "$name\n${value.toLong().minutes.formatOverview()}"
                        }
                    }
                val pieChart =
                    rememberPieChart(
                        sliceProvider = PieChart.SliceProvider.series(slices),
                        spacing = 0.dp,
                        outerSize = PieSize.Outer.fixed(148.dp),
                        innerSize = PieSize.Inner.fixed(120.dp),
                        startAngle = -90f + rotation.value,
                        valueFormatter = formatter,
                    )
                val model = remember(values) { PieChartModel.build(*values.toTypedArray()) }

                PieChartHost(
                    chart = pieChart,
                    model = model,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.CenterHorizontally)
                        .pointerInput(Unit) {
                            val tracker = VelocityTracker()
                            var angle = 0f
                            detectDragGestures(
                                onDragStart = {
                                    scope.launch { rotation.stop() }
                                    tracker.resetTracking()
                                    angle = rotation.value
                                },
                                onDragEnd = {
                                    val velocity = tracker.calculateVelocity().x
                                    scope.launch { rotation.animateDecay(velocity, decay) }
                                },
                            ) { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                angle += angleDelta(center, change.previousPosition, change.position)
                                tracker.addPosition(change.uptimeMillis, Offset(angle, 0f))
                                scope.launch { rotation.snapTo(angle) }
                                change.consume()
                            }
                        }.clickable(interactionSource = null, indication = null) {
                            showPercentages = !showPercentages
                        },
                )
            }
        }
    }
}

/** Returns the signed angle (in degrees) from [from] to [to] around [center], normalized to ±180°. */
private fun angleDelta(center: Offset, from: Offset, to: Offset): Float {
    val a = from - center
    val b = to - center
    var delta = (atan2(b.y, b.x) - atan2(a.y, a.x)) * 180f / PI.toFloat()
    if (delta > 180f) {
        delta -= 360f
    } else if (delta < -180f) {
        delta += 360f
    }
    return delta
}
