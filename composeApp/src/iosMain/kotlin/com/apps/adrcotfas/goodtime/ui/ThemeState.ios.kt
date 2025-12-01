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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.onboarding.MainUiState

/**
 * iOS implementation of collectThemeSettings.
 * TODO: Implement proper iOS system appearance detection on Mac.
 * Currently returns a basic implementation using only user preferences.
 */
@Composable
actual fun collectThemeSettings(
    mainUiState: MainUiState,
    showOnboarding: Boolean,
): State<ThemeSettings> {
    // Stub implementation - to be completed on Mac
    // TODO: Detect iOS system dark mode using UITraitCollection
    val systemDark = false // Placeholder

    return remember(mainUiState, showOnboarding) {
        mutableStateOf(
            ThemeSettings(
                darkTheme =
                    mainUiState.darkThemePreference.isDarkTheme(systemDark) &&
                        !showOnboarding,
                isDynamicTheme = mainUiState.isDynamicColor,
            ),
        )
    }
}
