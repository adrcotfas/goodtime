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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.model.Label
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore
import kotlin.time.Duration.Companion.minutes

val labelsKey = ExtraStore.Key<Set<String>>()

class HistoryBarChartMarkerValueFormatter(
    private val defaultLabelName: String,
    private val othersLabelName: String,
    private val othersLabelColor: Color,
    private val isTimeOverviewType: Boolean,
    private val totalLabel: String,
) : DefaultCartesianMarker.ValueFormatter {
    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence {
        val labels = context.model.extraStore[labelsKey]
        return buildAnnotatedString {
            targets.forEachIndexed { index, target ->
                appendTarget(target, labels)
                if (index != targets.lastIndex) append(", ")
            }
        }
    }

    private fun AnnotatedString.Builder.appendTarget(
        target: CartesianMarker.Target,
        labels: Set<String>,
    ) {
        when (target) {
            is ColumnCartesianLayerMarkerTarget -> {
                if (target.columns.all { it.entry.y == 0.0 }) return
                val includeSum = target.columns.count { it.entry.y > 0 } > 1
                if (includeSum) {
                    append("$totalLabel: ")
                    appendValue(target.columns.sumOf { it.entry.y })
                    append("\n")
                }
                val lastColumn = target.columns.last { it.entry.y > 0 }
                target.columns.forEachIndexed { index, column ->
                    if (column.entry.y > 0) {
                        val label =
                            labels.elementAtOrNull(index)?.let {
                                val localizedName =
                                    when (it) {
                                        Label.DEFAULT_LABEL_NAME -> defaultLabelName to column.color
                                        Label.OTHERS_LABEL_NAME -> othersLabelName to othersLabelColor
                                        else -> it to column.color
                                    }
                                "${localizedName.first}: " to localizedName.second
                            } ?: ("" to null)

                        val labelColor = label.second
                        if (labelColor != null) {
                            withStyle(SpanStyle(color = labelColor)) {
                                append(label.first)
                                append(" ")
                                appendValue(column.entry.y)
                            }
                        } else {
                            append(label.first)
                            append(" ")
                            appendValue(column.entry.y)
                        }

                        if (column != lastColumn) append("\n")
                    }
                }
            }

            else -> throw IllegalArgumentException("Unexpected `CartesianMarker.Target` implementation.")
        }
    }

    private fun AnnotatedString.Builder.appendValue(y: Double) {
        val valueFormatted =
            if (isTimeOverviewType) y.minutes.formatOverview() else y.toInt().toString()
        append(valueFormatted)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is HistoryBarChartMarkerValueFormatter &&
            defaultLabelName == other.defaultLabelName &&
            othersLabelName == other.othersLabelName &&
            othersLabelColor == other.othersLabelColor &&
            totalLabel == other.totalLabel

    override fun hashCode(): Int =
        defaultLabelName.hashCode() * 31 + othersLabelName.hashCode() + othersLabelColor.hashCode() + totalLabel.hashCode()
}
