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

import android.content.res.Configuration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Configuration.isPortrait: Boolean
    get() = this.orientation == Configuration.ORIENTATION_PORTRAIT

val Configuration.screenWidth: Dp
    get() = if (isPortrait) screenWidthDp.dp else screenHeightDp.dp

val Configuration.screenHeight: Dp
    get() = if (isPortrait) screenHeightDp.dp else screenWidthDp.dp

fun convertSpToDp(
    density: Density,
    spValue: Float,
): Float = with(density) { spValue.sp.toDp().value }
