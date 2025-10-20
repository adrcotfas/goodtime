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
package com.apps.adrcotfas.goodtime.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.apps.adrcotfas.goodtime.common.screenHeight
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun ScreensaverMode(
    screensaverMode: Boolean,
    isActive: Boolean,
    screenWidth: Dp,
    yOffset: Animatable<Float, AnimationVector1D>,
) {
    val coroutineScope = rememberCoroutineScope()
    var animationJob: Job? by remember { mutableStateOf(null) }
    val configuration = LocalConfiguration.current

    LaunchedEffect(screensaverMode && isActive) {
        if (screensaverMode && isActive) {
            animationJob =
                coroutineScope.launch {
                    while (true) {
                        delay(30.seconds)
                        val max = (configuration.screenHeight - screenWidth) / 3
                        val maxValue = abs(max.value.toInt())
                        if (maxValue > 0) {
                            val newOffset = Random.nextInt(from = -maxValue, until = maxValue)
                            yOffset.animateTo(
                                newOffset.toFloat(),
                                animationSpec = tween(durationMillis = 2000, easing = EaseInOut),
                            )
                        }
                    }
                }
        } else {
            animationJob?.cancel()
            yOffset.animateTo(
                0f,
                animationSpec = tween(durationMillis = 150, easing = EaseInOut),
            )
        }
    }
}
