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
package com.apps.adrcotfas.goodtime.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apps.adrcotfas.goodtime.ui.ActionCard
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_allow
import goodtime_productivity.composeapp.generated.resources.settings_allow_notifications
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberPermissionsState(): PermissionsState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var shouldAskForNotificationPermission by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                checkNotificationPermissionStatus { shouldAsk ->
                    shouldAskForNotificationPermission = shouldAsk
                }
            }
            else -> {
                // do nothing
            }
        }
    }

    return PermissionsState(
        shouldAskForNotificationPermission = shouldAskForNotificationPermission,
        shouldAskForBatteryOptimizationRemoval = false, // Not applicable on iOS
        shouldAskForAlarmPermission = false, // Not applicable on iOS
    )
}

@Composable
actual fun ActionsContent(
    permissionsState: PermissionsState,
    wasNotificationPermissionDenied: Boolean,
    onNotificationPermissionGranted: (Boolean) -> Unit,
) {
    AnimatedVisibility(permissionsState.shouldAskForNotificationPermission) {
        ActionCard(
            cta = stringResource(Res.string.settings_allow),
            description = stringResource(Res.string.settings_allow_notifications),
            onClick = {
                if (wasNotificationPermissionDenied) {
                    // Open app settings if permission was previously denied
                    openAppSettings()
                } else {
                    // Request notification permission
                    requestNotificationPermission { granted ->
                        onNotificationPermissionGranted(granted)
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun checkNotificationPermissionStatus(callback: (Boolean) -> Unit) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.getNotificationSettingsWithCompletionHandler { settings ->
        val shouldAsk =
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusNotDetermined -> true
                UNAuthorizationStatusDenied -> true
                UNAuthorizationStatusAuthorized -> false
                UNAuthorizationStatusProvisional -> false
                else -> false
            }
        callback(shouldAsk)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun requestNotificationPermission(callback: (Boolean) -> Unit) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge

    center.requestAuthorizationWithOptions(options) { granted, error ->
        callback(granted)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun openAppSettings() {
    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    settingsUrl?.let {
        if (UIApplication.sharedApplication.canOpenURL(it)) {
            UIApplication.sharedApplication.openURL(it)
        }
    }
}
