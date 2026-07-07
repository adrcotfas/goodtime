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
package com.apps.adrcotfas.goodtime.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min

/**
 * Returns whether the device is in portrait orientation.
 * This checks if the window height is greater than width.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun isPortrait(): Boolean {
    val windowInfo = LocalWindowInfo.current
    return windowInfo.containerSize.height > windowInfo.containerSize.width
}

/**
 * Returns the screen width in Dp, accounting for device orientation.
 * Always returns the smaller dimension (portrait-equivalent width).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun screenWidth(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) {
        min(containerSize.width, containerSize.height).toDp()
    }
}

/**
 * Returns the screen height in Dp, accounting for device orientation.
 * Always returns the larger dimension (portrait-equivalent height).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun screenHeight(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) {
        max(containerSize.width, containerSize.height).toDp()
    }
}
