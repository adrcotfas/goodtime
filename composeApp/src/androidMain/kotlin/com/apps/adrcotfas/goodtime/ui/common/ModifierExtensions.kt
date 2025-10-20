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
package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager

@Composable
@Stable
fun Modifier.hideUnless(condition: Boolean): Modifier {
    val alpha by animateFloatAsState(if (condition) 1f else 0f, label = "hide")
    return this then Modifier.graphicsLayer { this.alpha = alpha }
}

@OptIn(ExperimentalLayoutApi::class)
@Stable
fun Modifier.clearFocusOnKeyboardDismiss(onFocusCleared: () -> Unit = {}): Modifier =
    composed {
        var isFocused by remember { mutableStateOf(false) }
        var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }

        if (isFocused) {
            val imeIsVisible = WindowInsets.isImeVisible
            val focusManager = LocalFocusManager.current

            LaunchedEffect(imeIsVisible) {
                if (imeIsVisible) {
                    keyboardAppearedSinceLastFocused = true
                } else if (keyboardAppearedSinceLastFocused) {
                    focusManager.clearFocus()
                }
            }
        }

        onFocusEvent {
            if (isFocused != it.isFocused) {
                if (!it.isFocused) {
                    onFocusCleared()
                }
                isFocused = it.isFocused
                if (isFocused) keyboardAppearedSinceLastFocused = false
            }
        }
    }
