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
import com.apps.adrcotfas.goodtime.common.screenHeight
import com.apps.adrcotfas.goodtime.common.screenWidth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun ScreensaverMode(
    enabled: Boolean,
    isTimerActive: Boolean,
    yOffset: Animatable<Float, AnimationVector1D>,
) {
    val coroutineScope = rememberCoroutineScope()
    var animationJob: Job? by remember { mutableStateOf(null) }
    val screenWidth = screenWidth()
    val screenHeight = screenHeight()

    LaunchedEffect(enabled && isTimerActive) {
        if (enabled && isTimerActive) {
            animationJob =
                coroutineScope.launch {
                    while (true) {
                        delay(30.seconds)
                        val max = (screenHeight - screenWidth) / 3
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
