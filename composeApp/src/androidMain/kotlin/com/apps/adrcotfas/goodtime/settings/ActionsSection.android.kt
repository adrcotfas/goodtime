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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apps.adrcotfas.goodtime.common.areNotificationsEnabled
import com.apps.adrcotfas.goodtime.common.askForAlarmPermission
import com.apps.adrcotfas.goodtime.common.askForDisableBatteryOptimization
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.common.isIgnoringBatteryOptimizations
import com.apps.adrcotfas.goodtime.common.shouldAskForAlarmPermission
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_allow
import goodtime_productivity.composeapp.generated.resources.settings_allow_alarms
import goodtime_productivity.composeapp.generated.resources.settings_allow_background
import goodtime_productivity.composeapp.generated.resources.settings_allow_notifications
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun rememberPermissionsState(): PermissionsState {
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

@Composable
actual fun ActionsContent(
    permissionsState: PermissionsState,
    wasNotificationPermissionDenied: Boolean,
    onNotificationPermissionGranted: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            onNotificationPermissionGranted(granted)
        }

    AnimatedVisibility(permissionsState.shouldAskForAlarmPermission) {
        com.apps.adrcotfas.goodtime.ui.ActionCard(
            cta = stringResource(Res.string.settings_allow),
            description = stringResource(Res.string.settings_allow_alarms),
            onClick = { context.askForAlarmPermission() },
        )
    }
    AnimatedVisibility(permissionsState.shouldAskForBatteryOptimizationRemoval) {
        com.apps.adrcotfas.goodtime.ui.ActionCard(
            cta = stringResource(Res.string.settings_allow),
            description = stringResource(Res.string.settings_allow_background),
            onClick = { context.askForDisableBatteryOptimization() },
        )
    }

    AnimatedVisibility(permissionsState.shouldAskForNotificationPermission) {
        com.apps.adrcotfas.goodtime.ui.ActionCard(
            cta = stringResource(Res.string.settings_allow),
            description = stringResource(Res.string.settings_allow_notifications),
            onClick = {
                if (wasNotificationPermissionDenied &&
                    !shouldShowRequestPermissionRationale(
                        context.findActivity()!!,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                ) {
                    navigateToNotificationSettings(context)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    navigateToNotificationSettings(context)
                }
            },
        )
    }
}

private fun navigateToNotificationSettings(context: Context) {
    val intent =
        Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    context.startActivity(intent)
}
