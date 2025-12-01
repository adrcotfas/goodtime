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
import com.apps.adrcotfas.goodtime.onboarding.MainUiState

/**
 * Platform-specific function to collect theme settings.
 * Combines system dark mode detection with user preferences.
 *
 * Android: Uses Configuration listener to detect system dark mode changes
 * iOS: Uses iOS-specific system appearance detection
 *
 * @param mainUiState The main UI state containing user theme preferences
 * @param showOnboarding Whether the onboarding screen is shown (onboarding is always light theme)
 * @return A State containing the resolved ThemeSettings
 */
@Composable
expect fun collectThemeSettings(
    mainUiState: MainUiState,
    showOnboarding: Boolean,
): State<ThemeSettings>
