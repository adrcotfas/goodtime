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

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.ui.CheckboxListItem
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_click_to_grant_permission
import goodtime_productivity.composeapp.generated.resources.settings_do_not_disturb_mode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
actual fun DndCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val notificationManager = koinInject<NotificationArchManager>()
    var isNotificationPolicyAccessGranted by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted()) }
    var isNotificationPolicyAccessRequested by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                isNotificationPolicyAccessGranted =
                    notificationManager.isNotificationPolicyAccessGranted()
                if (isNotificationPolicyAccessRequested && isNotificationPolicyAccessGranted) {
                    onCheckedChange(true)
                }
                if (!isNotificationPolicyAccessGranted) {
                    onCheckedChange(false)
                }
            }

            else -> {
                // do nothing
            }
        }
    }

    CheckboxListItem(
        title = stringResource(Res.string.settings_do_not_disturb_mode),
        subtitle = if (isNotificationPolicyAccessGranted) null else stringResource(Res.string.settings_click_to_grant_permission),
        checked = checked,
    ) {
        if (isNotificationPolicyAccessGranted) {
            onCheckedChange(it)
        } else {
            isNotificationPolicyAccessRequested = true
            requestDndPolicyAccess(context.findActivity()!!)
        }
    }
}

private fun requestDndPolicyAccess(activity: ComponentActivity) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    activity.startActivity(intent)
}
