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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.apps.adrcotfas.goodtime.common.askForAlarmPermission
import com.apps.adrcotfas.goodtime.ui.SnackbarAction
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_allow
import goodtime_productivity.composeapp.generated.resources.settings_allow_alarms
import org.jetbrains.compose.resources.getString

@Composable
actual fun rememberAlarmPermissionRequester(permissionState: PermissionsState): suspend () -> Boolean {
    val context = LocalContext.current
    return remember(permissionState.shouldAskForAlarmPermission, context) {
        suspend {
            if (permissionState.shouldAskForAlarmPermission) {
                SnackbarController.sendEvent(
                    event =
                        SnackbarEvent(
                            message = getString(Res.string.settings_allow_alarms),
                            action =
                                SnackbarAction(
                                    name = getString(Res.string.settings_allow),
                                    action = {
                                        context.askForAlarmPermission()
                                    },
                                ),
                        ),
                )
                true
            } else {
                false
            }
        }
    }
}
