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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun getPermissionsState(): PermissionsState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    // Start with true to show the badge, hide it only if we confirm permission is granted
    var shouldAskForNotificationPermission by remember { mutableStateOf(true) }

    // Check permission status on first composition and when lifecycle changes
    LaunchedEffect(Unit, lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED || lifecycleState == Lifecycle.State.STARTED) {
            checkNotificationPermissionStatus { shouldAsk ->
                shouldAskForNotificationPermission = shouldAsk
            }
        }
    }

    return PermissionsState(
        shouldAskForNotificationPermission = shouldAskForNotificationPermission,
        shouldAskForBatteryOptimizationRemoval = false, // Not applicable on iOS
        shouldAskForAlarmPermission = false, // Not applicable on iOS
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun checkNotificationPermissionStatus(callback: (Boolean) -> Unit) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.getNotificationSettingsWithCompletionHandler { settings ->
        val status = settings?.authorizationStatus
        val shouldAsk = status == UNAuthorizationStatusNotDetermined || status == UNAuthorizationStatusDenied
        callback(shouldAsk)
    }
}
