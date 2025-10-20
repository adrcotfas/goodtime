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

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.model.Label
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlin.time.Duration.Companion.minutes

val labelsKey = ExtraStore.Key<Set<String>>()

class HistoryBarChartMarkerValueFormatter(
    private val defaultLabelName: String,
    private val othersLabelName: String,
    @ColorInt private val othersLabelColor: Int,
    private val isTimeOverviewType: Boolean,
    private val totalLabel: String,
) : DefaultCartesianMarker.ValueFormatter {
    private fun SpannableStringBuilder.append(
        y: Double,
        color: Int? = null,
    ) {
        val valueFormatted =
            if (isTimeOverviewType) y.minutes.formatOverview() else y.toInt().toString()
        if (color != null) {
            appendCompat2(
                valueFormatted,
                ForegroundColorSpan(color),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            append(valueFormatted)
        }
    }

    private fun SpannableStringBuilder.append(
        text: String,
        color: Int? = null,
    ) {
        if (color != null) {
            appendCompat2(
                text,
                ForegroundColorSpan(color),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            append(text)
        }
    }

    private fun SpannableStringBuilder.append(
        target: CartesianMarker.Target,
        labels: Set<String> = emptySet(),
    ) {
        when (target) {
            is ColumnCartesianLayerMarkerTarget -> {
                if (target.columns.all { it.entry.y == 0.0 }) return
                val includeSum = target.columns.count { it.entry.y > 0 } > 1
                if (includeSum) {
                    append("$totalLabel: ")
                    append(target.columns.sumOf { it.entry.y })
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
                        append(label.first, label.second)
                        append(" ")
                        append(column.entry.y, label.second)
                        if (column != lastColumn) append("\n")
                    }
                }
            }
            else -> throw IllegalArgumentException("Unexpected `CartesianMarker.Target` implementation.")
        }
    }

    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence {
        val labels = context.model.extraStore[labelsKey]
        return SpannableStringBuilder().apply {
            targets.forEachIndexed { index, target ->
                append(target = target, labels = labels)
                if (index != targets.lastIndex) append(", ")
            }
        }
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

internal fun SpannableStringBuilder.appendCompat2(
    text: CharSequence,
    what: Any,
    flags: Int,
): SpannableStringBuilder {
    append(text, 0, text.length)
    setSpan(what, length - text.length, length, flags)
    return this
}
