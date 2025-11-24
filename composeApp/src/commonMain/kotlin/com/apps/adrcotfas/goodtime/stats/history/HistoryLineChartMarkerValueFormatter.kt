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

import com.apps.adrcotfas.goodtime.common.formatOverview
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.LineCartesianLayerMarkerTarget
import kotlin.time.Duration.Companion.minutes

class HistoryLineChartMarkerValueFormatter(
    private val isTimeOverviewType: Boolean,
) : DefaultCartesianMarker.ValueFormatter {
    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ): CharSequence {
        targets.forEachIndexed { _, target ->
            when (target) {
                is LineCartesianLayerMarkerTarget -> {
                    if (target.points.all { it.entry.y == 0.0 }) return ""
                    val entry =
                        target.points
                            .first()
                            .entry.y
                    return if (isTimeOverviewType) {
                        entry.minutes.formatOverview()
                    } else {
                        entry.toInt().toString()
                    }
                }
            }
        }
        return ""
    }
}
