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
package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 8.dp,
    cap: StrokeCap = StrokeCap.Round,
) = dashedBorder(brush = SolidColor(color), shape, strokeWidth, dashLength, gapLength, cap)

fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 8.dp,
    cap: StrokeCap = StrokeCap.Round,
) = this.drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, density = this)

    val dashedStroke =
        Stroke(
            cap = cap,
            width = strokeWidth.toPx(),
            pathEffect =
                PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                ),
        )

    drawContent()

    drawOutline(
        outline = outline,
        style = dashedStroke,
        brush = brush,
    )
}
