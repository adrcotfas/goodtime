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
package com.apps.adrcotfas.goodtime.settings.timerstyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.screenWidth
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.ui.timerTextRobotoStyle
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun InitTimerStyle(viewModel: TimerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isLoading) return

    val timerStyle = uiState.timerStyle
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidth

    if (timerStyle.fontSize == 0f || abs(screenWidth.value - timerStyle.currentScreenWidth) > 64) {
        val maxContainerWidth = screenWidth * 0.75f
        val timerTextSize = findMaxFontSize(timerTextRobotoStyle(), maxContainerWidth)
        viewModel.initTimerStyle(
            maxSize = timerTextSize.em.value,
            screenWidth = screenWidth.value,
        )
    }
}

@Composable
private fun findMaxFontSize(
    style: TextStyle,
    containerWidth: Dp,
): Float {
    var currentFontSize = style.fontSize
    var textWidth = measureTextWidth(style.copy(fontSize = currentFontSize))
    while (textWidth > containerWidth) {
        currentFontSize *= 0.95f
        textWidth = measureTextWidth(style.copy(fontSize = currentFontSize))
    }
    return floor(currentFontSize.value)
}

@Composable
private fun measureTextWidth(style: TextStyle): Dp {
    val text = "90:00"
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}
