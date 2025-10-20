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

import android.text.Layout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.common.component.fixed
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shadow
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.shape.markerCorneredShape
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerMargins
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
internal fun rememberMarker(
    valueFormatter: DefaultCartesianMarker.ValueFormatter =
        DefaultCartesianMarker.ValueFormatter.default(),
): CartesianMarker {
    val labelBackgroundShape =
        markerCorneredShape(
            CorneredShape.Corner.Relative(
                sizePercent = 12,
                treatment = CorneredShape.CornerTreatment.Rounded,
            ),
        )
    val labelBackground =
        rememberShapeComponent(
            fill = fill(MaterialTheme.colorScheme.surfaceContainer),
            shape = labelBackgroundShape,
            shadow =
                shadow(
                    radius = LABEL_BACKGROUND_SHADOW_RADIUS_DP.dp,
                    y = LABEL_BACKGROUND_SHADOW_DY_DP.dp,
                ),
        )
    val label =
        rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            textAlignment = Layout.Alignment.ALIGN_OPPOSITE,
            lineCount = 10, // 1 for total, 1 for others, 8 for labels
            lineHeight = 16.0.sp,
            padding = insets(16.dp, 16.dp),
            background = labelBackground,
            minWidth = TextComponent.MinWidth.fixed(40.dp),
        )
    val guideline = rememberAxisGuidelineComponent()
    return remember(label, valueFormatter, guideline) {
        object :
            DefaultCartesianMarker(
                label = label,
                valueFormatter = valueFormatter,
                indicator = null,
                guideline = guideline,
            ) {
            override fun updateLayerMargins(
                context: CartesianMeasuringContext,
                layerMargins: CartesianLayerMargins,
                layerDimensions: CartesianLayerDimensions,
                model: CartesianChartModel,
            ) {
                with(context) {
                    val baseShadowMarginDp =
                        CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS_DP
                    val topMargin = (baseShadowMarginDp - LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                    val bottomMargin = (baseShadowMarginDp + LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                    layerMargins.ensureValuesAtLeast(top = topMargin, bottom = bottomMargin)
                }
            }
        }
    }
}

private const val LABEL_BACKGROUND_SHADOW_RADIUS_DP = 2f
private const val LABEL_BACKGROUND_SHADOW_DY_DP = 1f
private const val CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER = 1.4f
