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
package com.apps.adrcotfas.goodtime.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.apps.adrcotfas.goodtime.R

val bodyFontFamily = FontFamily(Font(resId = R.font.open_sans))

val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge = baseline.displayLarge,
        displayMedium = baseline.displayMedium,
        displaySmall = baseline.displaySmall,
        headlineLarge = baseline.headlineLarge,
        headlineMedium = baseline.headlineMedium,
        headlineSmall = baseline.headlineSmall,
        titleLarge = baseline.titleLarge.copy(fontFamily = bodyFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = bodyFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = bodyFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge,
        labelMedium = baseline.labelMedium,
        labelSmall = baseline.labelSmall,
    )

@OptIn(ExperimentalTextApi::class)
fun timerFontWith(
    resId: Int,
    weight: Int,
): FontFamily =
    FontFamily(
        Font(
            resId = resId,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )

val timerFontWeights = listOf(100, 200, 300)

// TODO: remove unused glyphs from the font
val timerFontRobotoMap =
    timerFontWeights.associateWith { weight -> timerFontWith(R.font.roboto_mono, weight) }

val timerTextRobotoStyle =
    TextStyle(
        fontFamily = timerFontRobotoMap[100],
        fontSize = 60.em,
    )
