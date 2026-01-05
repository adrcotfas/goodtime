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
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.google_sans_medium
import goodtime_productivity.composeapp.generated.resources.google_sans_regular
import goodtime_productivity.composeapp.generated.resources.roboto_mono
import org.jetbrains.compose.resources.Font

@Composable
fun bodyFontFamily() =
    FontFamily(
        Font(Res.font.google_sans_regular, FontWeight.Normal),
        Font(Res.font.google_sans_medium, FontWeight.Medium),
    )

val baseline = Typography()

@Composable
fun appTypography(): Typography {
    val appFont = bodyFontFamily()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = appFont),
        displayMedium = baseline.displayMedium.copy(fontFamily = appFont),
        displaySmall = baseline.displaySmall.copy(fontFamily = appFont, letterSpacing = 0.1.sp),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = appFont),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = appFont),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = appFont),
        titleLarge = baseline.titleLarge.copy(fontFamily = appFont),
        titleMedium = baseline.titleMedium.copy(fontFamily = appFont),
        titleSmall = baseline.titleSmall.copy(fontFamily = appFont),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = appFont),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = appFont, letterSpacing = 0.6.sp),
        bodySmall = baseline.bodySmall.copy(fontFamily = appFont),
        labelLarge = baseline.labelLarge,
        labelMedium = baseline.labelMedium,
        labelSmall = baseline.labelSmall,
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun timerFontWith(
    resource: org.jetbrains.compose.resources.FontResource,
    weight: Int,
): FontFamily =
    FontFamily(
        Font(
            resource = resource,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )

val timerFontWeights = listOf(100, 200, 300)

@Composable
fun timerFontRobotoMap(): Map<Int, FontFamily> = timerFontWeights.associateWith { weight -> timerFontWith(Res.font.roboto_mono, weight) }

@Composable
fun timerTextRobotoStyle(): TextStyle {
    val fontMap = timerFontRobotoMap()
    return TextStyle(
        fontFamily = fontMap[100],
        fontSize = 60.em,
    )
}
