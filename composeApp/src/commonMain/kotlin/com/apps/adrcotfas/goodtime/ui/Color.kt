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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun String.toColorInt(): Int {
    var color = substring(1).toLong(16)
    if (length == 7) {
        // Set the alpha value.
        color = color or 0x00000000FF000000L
    }
    return color.toInt()
}

val primaryLight = Color(0xFF176B53)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFA5F2D4)
val onPrimaryContainerLight = Color(0xFF00513D)
val secondaryLight = Color(0xFF4C6359)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFCEE9DC)
val onSecondaryContainerLight = Color(0xFF344C42)
val tertiaryLight = Color(0xFF006A63)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFF9DF2E8)
val onTertiaryContainerLight = Color(0xFF00504A)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = Color(0xFFF5FBF6)
val onBackgroundLight = Color(0xFF171D1A)
val surfaceLight = Color(0xFFF5FBF6)
val onSurfaceLight = Color(0xFF171D1A)
val surfaceVariantLight = Color(0xFFDBE5DE)
val onSurfaceVariantLight = Color(0xFF404944)
val outlineLight = Color(0xFF707974)
val outlineVariantLight = Color(0xFFBFC9C3)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF2C322F)
val inverseOnSurfaceLight = Color(0xFFECF2ED)
val inversePrimaryLight = Color(0xFF89D6B8)
val surfaceDimLight = Color(0xFFD5DBD7)
val surfaceBrightLight = Color(0xFFF5FBF6)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFEFF5F0)
val surfaceContainerLight = Color(0xFFE9EFEA)
val surfaceContainerHighLight = Color(0xFFE4EAE5)
val surfaceContainerHighestLight = Color(0xFFDEE4DF)

val primaryDark = Color(0xFF89D6B8)
val onPrimaryDark = Color(0xFF003829)
val primaryContainerDark = Color(0xFF00513D)
val onPrimaryContainerDark = Color(0xFFA5F2D4)
val secondaryDark = Color(0xFFB2CCC0)
val onSecondaryDark = Color(0xFF1E352C)
val secondaryContainerDark = Color(0xFF344C42)
val onSecondaryContainerDark = Color(0xFFCEE9DC)
val tertiaryDark = Color(0xFF81D5CC)
val onTertiaryDark = Color(0xFF003733)
val tertiaryContainerDark = Color(0xFF00504A)
val onTertiaryContainerDark = Color(0xFF9DF2E8)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF000000)
val onBackgroundDark = Color(0xFFDEE4DF)
val surfaceDark = Color(0xFF000000)
val onSurfaceDark = Color(0xFFDEE4DF)
val surfaceVariantDark = Color(0xFF404944)
val onSurfaceVariantDark = Color(0xFFBFC9C3)
val outlineDark = Color(0xFF89938D)
val outlineVariantDark = Color(0xFF404944)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFDEE4DF)
val inverseOnSurfaceDark = Color(0xFF2C322F)
val inversePrimaryDark = Color(0xFF176B53)
val surfaceDimDark = Color(0xFF0F1512)
val surfaceBrightDark = Color(0xFF343B37)
val surfaceContainerLowestDark = Color(0xFF0A0F0D)
val surfaceContainerLowDark = Color(0xFF171D1A)
val surfaceContainerDark = Color(0xFF1B211E)
val surfaceContainerHighDark = Color(0xFF252B28)
val surfaceContainerHighestDark = Color(0xFF303633)

val MaterialTheme.localColorsPalette: CustomColorsPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalColorsPalette.current

val lightColorScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

val darkColorScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

@Immutable
data class CustomColorsPalette(
    val colors: List<Color> = listOf(Color.Unspecified),
)

val LocalColorsPalette = staticCompositionLocalOf { CustomColorsPalette() }

val LightColorsPalette =
    CustomColorsPalette(lightPalette.map { Color(it.toColorInt()) })
val DarkColorsPalette =
    CustomColorsPalette(darkPalette.map { Color(it.toColorInt()) })
