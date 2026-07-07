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

/**
 * Platform-specific language settings item.
 * On Android: shows a clickable item that opens system language settings
 * On iOS: empty (not supported)
 */
@Composable
expect fun LanguageSettingsItem()

/**
 * Platform-specific dynamic color checkbox.
 * On Android 12+: shows checkbox to toggle Material You dynamic colors
 * On iOS: empty (not supported)
 */
@Composable
expect fun DynamicColorCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
)

/**
 * Platform-specific launcher name dropdown.
 * On Android: shows dropdown to change the app's launcher name
 * On iOS: empty (not supported)
 */
@Composable
expect fun LauncherNameDropdown(
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
)
