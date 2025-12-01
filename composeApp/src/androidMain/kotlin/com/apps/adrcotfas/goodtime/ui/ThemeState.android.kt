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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.onboarding.MainUiState
import kotlinx.coroutines.flow.combine

/**
 * Android implementation of collectThemeSettings.
 * Combines system dark mode detection (via Configuration listener) with user preferences.
 */
@Composable
actual fun collectThemeSettings(
    mainUiState: MainUiState,
    showOnboarding: Boolean,
): State<ThemeSettings> {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    return produceState(
        initialValue =
            ThemeSettings(
                darkTheme = false,
                isDynamicTheme = false,
            ),
    ) {
        if (activity != null) {
            combine(
                activity.isSystemInDarkTheme(),
                kotlinx.coroutines.flow.flowOf(mainUiState),
            ) { systemDark, uiState ->
                ThemeSettings(
                    darkTheme =
                        uiState.darkThemePreference.isDarkTheme(systemDark) &&
                            !showOnboarding,
                    isDynamicTheme = uiState.isDynamicColor,
                )
            }.collect { value = it }
        }
    }
}
