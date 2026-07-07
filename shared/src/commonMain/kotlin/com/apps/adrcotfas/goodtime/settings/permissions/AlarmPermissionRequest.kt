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
package com.apps.adrcotfas.goodtime.settings.permissions

import androidx.compose.runtime.Composable

/**
 * Requests alarm permission if needed for the current platform.
 * @param permissionState The current permission state
 * @return A function that, when called, will request the permission if needed.
 *         Returns true if permission was requested (don't start timer),
 *         false if no permission needed (start timer).
 */
@Composable
expect fun rememberAlarmPermissionRequester(permissionState: PermissionsState): suspend () -> Boolean
