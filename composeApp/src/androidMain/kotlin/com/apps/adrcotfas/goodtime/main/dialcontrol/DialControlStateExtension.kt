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
package com.apps.adrcotfas.goodtime.main.dialcontrol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.apps.adrcotfas.goodtime.bl.isFocus
import com.apps.adrcotfas.goodtime.main.TimerUiState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberCustomDialControlState(
    density: Density = LocalDensity.current,
    config: DialConfig = DialConfig(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onLeft: () -> Unit,
    onTop: () -> Unit,
    onRight: () -> Unit,
    onBottom: () -> Unit,
): DialControlState<DialRegion> =
    remember(density, config, coroutineScope) {
        DialControlState(
            initialOptions = DialRegion.entries,
            onSelected = {
                when (it) {
                    DialRegion.LEFT -> onLeft()
                    DialRegion.TOP -> onTop()
                    DialRegion.RIGHT -> onRight()
                    DialRegion.BOTTOM -> onBottom()
                }
            },
            config = config,
            density = density,
            coroutineScope = coroutineScope,
        )
    }

fun DialControlState<DialRegion>.updateEnabledOptions(timerUiState: TimerUiState) {
    val label = timerUiState.label
    val thereIsNoBreakBudget =
        timerUiState.breakBudgetMinutes == 0L
    val isCountUp = !label.profile.isCountdown
    val isCountUpWithoutBreaks = isCountUp && !label.profile.isBreakEnabled

    val showSkip =
        (isCountUp && thereIsNoBreakBudget && timerUiState.timerType.isFocus) ||
            isCountUpWithoutBreaks

    val disabledOptions =
        buildList {
            if (isCountUp) {
                add(DialRegion.TOP)
            }
            if (showSkip) {
                add(DialRegion.RIGHT)
                add(DialRegion.LEFT)
            }
        }
    updateEnabledOptions(disabledOptions)
}
