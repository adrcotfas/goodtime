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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apps.adrcotfas.goodtime.common.areNotificationsEnabled
import com.apps.adrcotfas.goodtime.common.isIgnoringBatteryOptimizations
import com.apps.adrcotfas.goodtime.common.shouldAskForAlarmPermission

data class PermissionsState(
    val shouldAskForNotificationPermission: Boolean,
    val shouldAskForBatteryOptimizationRemoval: Boolean,
    val shouldAskForAlarmPermission: Boolean,
) {
    fun count() =
        listOf(
            shouldAskForNotificationPermission,
            shouldAskForBatteryOptimizationRemoval,
            shouldAskForAlarmPermission,
        ).count { it }
}

@Composable
fun getPermissionsState(): PermissionsState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var shouldAskForNotificationPermission by remember { mutableStateOf(!context.areNotificationsEnabled()) }
    var shouldAskForBatteryOptimizationRemoval by remember { mutableStateOf(!context.isIgnoringBatteryOptimizations()) }
    var shouldAskForAlarmPermission by remember { mutableStateOf(context.shouldAskForAlarmPermission()) }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                shouldAskForNotificationPermission = !context.areNotificationsEnabled()
                shouldAskForBatteryOptimizationRemoval = !context.isIgnoringBatteryOptimizations()
                shouldAskForAlarmPermission = context.shouldAskForAlarmPermission()
            }

            else -> {
                // do nothing
            }
        }
    }
    return PermissionsState(
        shouldAskForNotificationPermission = shouldAskForNotificationPermission,
        shouldAskForBatteryOptimizationRemoval = shouldAskForBatteryOptimizationRemoval,
        shouldAskForAlarmPermission = shouldAskForAlarmPermission,
    )
}
