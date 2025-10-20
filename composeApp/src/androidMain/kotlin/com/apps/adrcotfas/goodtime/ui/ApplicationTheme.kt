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

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.BREAK_COLOR_INDEX
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.DEFAULT_LABEL_COLOR_INDEX

@Composable
fun ApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    val customColorsPalette =
        if (darkTheme) {
            LightColorsPalette
        } else {
            DarkColorsPalette
        }

    CompositionLocalProvider(LocalColorsPalette provides customColorsPalette) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content,
        )
    }
}

@Composable
fun MaterialTheme.getLabelColor(colorIndex: Long): Color {
    val colors = localColorsPalette.colors
    return if (colorIndex in colors.indices) {
        colors[colorIndex.toInt()]
    } else {
        colors[DEFAULT_LABEL_COLOR_INDEX]
    }
}

@Composable
fun MaterialTheme.breakColor(): Color = MaterialTheme.getLabelColor(BREAK_COLOR_INDEX.toLong())
