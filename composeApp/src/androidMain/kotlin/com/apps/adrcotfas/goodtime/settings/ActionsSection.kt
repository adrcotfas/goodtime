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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.askForAlarmPermission
import com.apps.adrcotfas.goodtime.common.askForDisableBatteryOptimization
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.settings.permissions.getPermissionsState
import com.apps.adrcotfas.goodtime.ui.common.ActionCard
import com.apps.adrcotfas.goodtime.ui.common.PreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider

@Composable
fun ActionSection(
    wasNotificationPermissionDenied: Boolean,
    onNotificationPermissionGranted: (Boolean) -> Unit,
    isUpdateAvailable: Boolean,
    onUpdateClicked: () -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            onNotificationPermissionGranted(granted)
        }

    val permissionsState = getPermissionsState()

    AnimatedVisibility(
        permissionsState.shouldAskForNotificationPermission || permissionsState.shouldAskForBatteryOptimizationRemoval || isUpdateAvailable,
    ) {
        Column {
            SubtleHorizontalDivider()
            Spacer(Modifier.height(8.dp))
            PreferenceGroupTitle(
                text = stringResource(R.string.settings_action_required),
                paddingValues =
                    PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
            )
            AnimatedVisibility(permissionsState.shouldAskForAlarmPermission) {
                ActionCard(
                    cta = stringResource(R.string.settings_allow),
                    description = stringResource(R.string.settings_allow_alarms),
                    onClick = { context.askForAlarmPermission() },
                )
            }
            AnimatedVisibility(permissionsState.shouldAskForBatteryOptimizationRemoval) {
                ActionCard(
                    cta = stringResource(R.string.settings_allow),
                    description = stringResource(R.string.settings_allow_background),
                    onClick = { context.askForDisableBatteryOptimization() },
                )
            }

            AnimatedVisibility(permissionsState.shouldAskForNotificationPermission) {
                ActionCard(
                    cta = stringResource(R.string.settings_allow),
                    description = stringResource(R.string.settings_allow_notifications),
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

            AnimatedVisibility(isUpdateAvailable) {
                ActionCard(
                    cta = stringResource(R.string.settings_update),
                    description = stringResource(R.string.settings_update_available),
                    onClick = onUpdateClicked,
                )
            }
            Spacer(Modifier.height(8.dp))
            SubtleHorizontalDivider()
        }
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
